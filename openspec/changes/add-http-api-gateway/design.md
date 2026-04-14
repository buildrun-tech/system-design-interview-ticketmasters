## Context

O microserviço roda em ECS Fargate, exposto internamente via NLB interno. O acesso ao NLB está restrito ao CIDR da VPC. Existe um `aws_api_gateway_vpc_link` (REST API v1) no módulo `ecs`, mas nenhum recurso de API Gateway foi criado — o VPC Link está "solto", sem consumidor.

A proposta migra para **HTTP API v2** (`aws_apigatewayv2_api`), cria um módulo dedicado `api-gateway`, move o VPC Link para dentro desse módulo, e aperta a cadeia de security groups para que o NLB aceite tráfego somente do VPC Link.

## Goals / Non-Goals

**Goals:**
- API Gateway HTTP API v2 público (internet-facing), proxy transparente para o ECS
- Módulo Terraform isolado `terraform/modules/api-gateway/`
- VPC Link v2 (`aws_apigatewayv2_vpc_link`) com Security Group próprio dentro do módulo
- NLB SG restrito ao SG do VPC Link (sem mais VPC CIDR aberto)
- Passagem de todos os paths, headers e query params sem modificação

**Non-Goals:**
- Custom domain / certificado TLS próprio (change futuro)
- Autenticação no API Gateway (feita pela app no ECS)
- Rate limiting, WAF, usage plans
- REST API v1 (descartado em favor de HTTP API v2)

## Decisions

### 1. HTTP API v2 em vez de REST API v1

**Escolhido**: `aws_apigatewayv2_api` (HTTP API)
**Alternativa descartada**: `aws_api_gateway_rest_api` (REST API)

**Razão**: HTTP API v2 é ~70% mais barato, tem latência menor, configuração de proxy mais simples (sem necessidade de `aws_api_gateway_method`, `aws_api_gateway_integration`, `aws_api_gateway_deployment` separados). O VPC Link v2 (`aws_apigatewayv2_vpc_link`) suporta Security Groups próprios, permitindo o amarramento de rede que a proposta requer. REST API v1 não suporta SG no VPC Link — o tráfego aparece como CIDR da VPC, impossibilitando isolamento granular.

### 2. Módulo dedicado `api-gateway`

**Escolhido**: novo `terraform/modules/api-gateway/`
**Alternativa descartada**: adicionar recursos no módulo `ecs` ou `networking`

**Razão**: O módulo `ecs` já é extenso e o API Gateway é uma responsabilidade distinta. Separar em módulo próprio segue o padrão já adotado (cada recurso AWS com responsabilidade clara tem seu módulo). O VPC Link v2 vive naturalmente junto com o API Gateway — é o consumidor direto.

### 3. VPC Link v2 dentro do módulo `api-gateway`

**Escolhido**: `aws_apigatewayv2_vpc_link` no módulo `api-gateway`
**Alternativa descartada**: manter VPC Link no módulo `ecs` ou `networking`

**Razão**: O VPC Link é um detalhe de integração do API Gateway com a rede privada. Semanticamente, pertence ao módulo que o consome. O `aws_api_gateway_vpc_link` (v1) legado no módulo `ecs` será removido — era um placeholder sem consumidor.

### 4. Rota catch-all `ANY /{proxy+}` com integração HTTP_PROXY

**Escolhido**: uma única rota `ANY /{proxy+}` + rota `ANY /` → integração `HTTP_PROXY` via VPC Link
**Alternativa descartada**: rotas individuais por path/método

**Razão**: O objetivo é proxy puro. Uma rota catch-all com `overwrite:path` no parâmetro de integração garante que todos os paths, métodos, headers e query strings sejam repassados sem modificação. Rotas individuais criariam acoplamento entre API Gateway e a API da aplicação — qualquer novo endpoint exigiria atualização no Terraform.

### 5. SG do VPC Link com egress restrito ao NLB SG

**Escolhido**: `apigw_vpc_link_sg` com egress `security_groups = [nlb_sg_id]` na porta 8080
**Alternativa descartada**: egress aberto (`0.0.0.0/0`)

**Razão**: O módulo `networking` expõe o `nlb_security_group_id` como output. Referenciar o SG por ID (não por CIDR) garante que, mesmo que os IPs do NLB mudem, a regra permanece válida. O NLB SG por sua vez passa a aceitar ingress somente desse SG — formando um canal exclusivo: `API GW → VPC Link SG → NLB SG → NLB → ECS SG → ECS`.

### 6. Stage `$default` com auto-deploy

**Escolhido**: stage `$default`, `auto_deploy = true`
**Alternativa descartada**: stage nomeado com deploy manual

**Razão**: Para esse cenário de proxy simples sem versionamento de API, o stage `$default` simplifica a configuração e garante que mudanças na integração sejam publicadas automaticamente. Deploy manual é overhead desnecessário quando não há múltiplos stages.

## Risks / Trade-offs

- **VPC Link demora ~2min para ficar AVAILABLE** → Mitigation: `terraform apply` vai aguardar; adicionar `depends_on` na integração do API Gateway para evitar race condition.
- **Remoção do `aws_api_gateway_vpc_link` do módulo `ecs` pode deixar estado órfão** → Mitigation: incluir `terraform state rm` na migration plan antes do apply; ou usar `removed {}` block do Terraform >= 1.7.
- **NLB SG deixa de aceitar VPC CIDR** → qualquer serviço interno que batia diretamente no NLB via CIDR da VPC perderá acesso. Mitigation: verificar se há outros consumers do NLB antes do apply (no projeto atual, não há).
- **HTTP API v2 não suporta algumas features do REST API v1** (ex: response mapping, caching, request validation no gateway) → Aceito; essas features não são necessárias nesse stage do projeto.

## Migration Plan

1. **Remover o VPC Link v1 do state Terraform do módulo `ecs`**:
   ```
   terraform state rm module.ecs.aws_api_gateway_vpc_link.main
   ```
   (ou usar `removed {}` block no código se Terraform >= 1.7)

2. **Aplicar o novo módulo `api-gateway`** — cria VPC Link v2, SG, HTTP API, integração, stage.

3. **Aplicar a mudança no NLB SG** (módulo `networking`) — ingress muda de VPC CIDR para `apigw_vpc_link_sg_id`.

4. **Verificar** endpoint do API Gateway via `terraform output api_gateway_endpoint` e testar uma chamada.

**Rollback**: Se necessário, reverter o NLB SG para VPC CIDR e destruir o módulo `api-gateway`. O módulo `ecs` pode ter o VPC Link v1 reinserido (ou deixado removido, já que não era consumido).

## Open Questions

- (Nenhuma — escopo bem definido. Custom domain e auth são changes futuras explicitamente fora de escopo.)
