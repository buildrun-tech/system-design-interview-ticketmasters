## Context

A aplicação Ticketmaster é um serviço Quarkus 3.26.4 / Java 21 rodando em ECS Fargate com autoscaling por CPU. Atualmente, observabilidade se limita a:
- CloudWatch Logs (driver `awslogs` no container)
- Container Insights (CPU/memória do container, nível ECS — não aplicação)

A `ecs_task_role` tem apenas permissões SQS. A `ecs_task_execution_role` tem ECR + Secrets Manager + logs básicos.

O Quarkus suporta nativamente Micrometer como façade de métricas e tem extensão `quarkus-opentelemetry` que integra o OTel SDK. O objetivo é: **App → Micrometer (API) → OTel SDK (bridge) → OTLP → ADOT Collector (sidecar) → CloudWatch EMF**.

## Goals / Non-Goals

**Goals:**
- Expor os 4 Golden Signals no CloudWatch com instrumentação automática dos endpoints HTTP
- Sem vendor lock-in na API de métricas do código (usar Micrometer como façade)
- Custo menor que Custom Metrics direto (usar EMF via CloudWatch Logs)
- Zero mudança no código de negócio (instrumentação automática do Quarkus)
- Dashboard operacional pronto para uso

**Non-Goals:**
- Tracing distribuído (pode vir depois com o mesmo pipeline OTel)
- Métricas de negócio customizadas (booking rate, etc.) — fase posterior
- Alertas PagerDuty / SNS — apenas alarmes CloudWatch básicos agora
- Mudança no autoscaling (permanece baseado em CPU ECS)

## Decisions

### D1: Micrometer como API + OTel SDK como bridge (não OTel direto)

**Decisão**: usar `quarkus-micrometer` como API de métricas e `quarkus-opentelemetry` com o OTel Micrometer bridge para exportar via OTLP.

**Alternativas consideradas**:
- OTel SDK direto sem Micrometer: perderia a instrumentação automática do Quarkus (HTTP server, JVM, DB pool) que já usa Micrometer
- Micrometer + CloudWatch2 registry direto (sem sidecar): mais simples, mas cria acoplamento direto App → CloudWatch, mais difícil de adicionar tracing depois, e custom metrics são mais caros que EMF

**Motivo**: Micrometer é o padrão de facto do ecossistema Quarkus/Spring. O bridge Micrometer→OTel SDK permite usar OTel como transporte sem mudar a API. Adicionar tracing depois é natural (mesmo sidecar, mesmo pipeline).

### D2: ADOT Collector como sidecar na Task Definition (não ECS Service Connect nem daemon)

**Decisão**: ADOT Collector no mesmo Task como segundo container, comunicando via `localhost:4317` (gRPC OTLP).

**Alternativas consideradas**:
- ADOT como ECS Daemon task: mais complexo de configurar, ainda precisaria de network entre tasks
- OTel Collector separado (task independente): latência de rede, custo de task extra, complexidade
- Sidecar na mesma task: comunicação via loopback (sem rede), mesmo ciclo de vida, simples

**Motivo**: Em Fargate com `awsvpc`, cada task tem seu próprio network namespace. Sidecar compartilha o namespace — comunicação via `localhost` sem overhead de rede, e o lifecycle é atrelado ao container principal.

### D3: ADOT Collector com exporter `awsemf` (CloudWatch EMF) em vez de `awscloudwatch`

**Decisão**: usar o exporter EMF (Embedded Metrics Format) que escreve logs JSON formatados para CloudWatch Logs, e o CloudWatch os converte em métricas.

**Alternativas consideradas**:
- `awscloudwatch` exporter: envia PutMetricData diretamente, Custom Metrics a $0.30/métrica/mês — com ~15 métricas, $4.50/mês só de métricas
- `awsemf` exporter: escreve para CloudWatch Logs ($0.50/GB ingestion), métricas derivadas via EMF são muito mais baratas para o volume típico

**Motivo**: EMF é o padrão recomendado pela AWS para métricas de aplicação em ECS. Container Insights já usa EMF internamente. Custo significativamente menor.

### D4: Configuração do ADOT Collector via SSM Parameter Store (não arquivo no container)

**Decisão**: a config YAML do ADOT Collector será passada via variável de ambiente `AOT_CONFIG_CONTENT` (SSM Parameter Store referenciado na Task Definition como secret).

**Alternativas consideradas**:
- Bake na imagem Docker do collector: precisa rebuild para mudar config
- ConfigMap / S3: complexidade adicional desnecessária
- SSM Parameter Store: permite alterar config sem rebuild, integra com a Task Definition como `secrets`

**Motivo**: O ADOT Collector suporta `AOT_CONFIG_CONTENT` nativamente. O padrão já existe no projeto (DB password via Secrets Manager). Reutiliza o pattern.

### D5: Métricas publicadas no namespace `TicketMaster/App`

**Decisão**: namespace CloudWatch customizado `TicketMaster/App` com dimensões `ServiceName` e `Environment`.

**Motivo**: Separação clara das métricas nativas do ECS (`AWS/ECS`). Facilita filtros de dashboard e alarmes por ambiente (staging/prod).

## Risks / Trade-offs

| Risco | Mitigação |
|-------|-----------|
| Sidecar ADOT usa CPU/memória extras na task | Definir limites conservadores (256 CPU units, 256MB) — monitorar e ajustar |
| Latência de startup: app inicia antes do collector | OTel SDK tem retry com backoff — métricas dos primeiros segundos podem ser perdidas, aceitável |
| SSM Parameter Store tem custo por API call | Task Definition cacheia secrets — sem impacto em runtime |
| EMF logs aumentam volume em CloudWatch Logs | Estimar: ~1KB/push × 60 pushes/hora × 730h/mês = ~43MB/mês por instância — negligível |
| `quarkus-opentelemetry` pode conflitar com dependências existentes | Quarkus 3.26.x já tem OTel integrado no BOM — sem conflito esperado |

## Migration Plan

1. **Fase 1 - App**: adicionar dependências no `pom.xml` e propriedades no `application.properties` — sem impacto em runtime se o collector não estiver disponível (OTel SDK falha silenciosamente com retry)
2. **Fase 2 - Infra (Terraform)**: criar SSM Parameter com config do ADOT, adicionar sidecar na Task Definition, adicionar policy IAM na task role
3. **Fase 3 - Validação**: verificar métricas chegando no namespace `TicketMaster/App` no CloudWatch console
4. **Fase 4 - Dashboard**: criar o CloudWatch Dashboard e alarmes via Terraform

**Rollback**: remover o container sidecar da Task Definition + reverter `pom.xml`. A app funciona normalmente sem o collector (falha de exportação não afeta o serviço principal).

## Open Questions

- Qual a CPU/memória alocada na Task Definition hoje (`task_cpu` e `task_memory`)? Preciso garantir que adicionar 256 CPU units e 256MB para o sidecar não exceda o total configurado.
- Existe ambiente de staging para validar antes de prod?
