## Why

A aplicação Ticketmaster roda em ECS Fargate mas não possui observabilidade de aplicação — apenas logs e métricas de infraestrutura (Container Insights). Sem os 4 Golden Signals (Latência, Tráfego, Erros, Saturação) não é possível detectar degradação de performance, correlacionar incidentes ou embasar decisões de scaling.

## What Changes

- Adição das dependências `quarkus-micrometer` e `quarkus-opentelemetry` no `pom.xml`
- Configuração do Micrometer para expor métricas via OTLP (bridging Micrometer → OTel SDK)
- Adição do container **AWS Distro for OpenTelemetry (ADOT) Collector** como sidecar na ECS Task Definition
- Configuração do ADOT Collector com pipeline: `otlp receiver → batch processor → awsemf exporter` (CloudWatch EMF)
- Adição da policy `cloudwatch:PutMetricData` e `logs:PutLogEvents` na `ecs_task_role`
- Configuração de um CloudWatch Dashboard com widgets para os 4 Golden Signals
- Criação de alarmes CloudWatch para Latência (p99 > threshold) e Error Rate (> threshold)

## Capabilities

### New Capabilities

- `app-metrics-instrumentation`: Instrumentação da aplicação Quarkus com Micrometer (HTTP latency, traffic, errors, JVM saturation, DB pool) exportando via OTLP para o sidecar collector
- `otel-collector-sidecar`: Container ADOT Collector na ECS Task Definition recebendo métricas OTLP da app e exportando para CloudWatch via EMF format
- `cloudwatch-golden-signals-dashboard`: Dashboard CloudWatch com widgets para Latência (p50/p95/p99), Tráfego (req/s), Erros (4xx/5xx rate), Saturação (JVM heap, DB pool, CPU)
- `iam-cloudwatch-policy`: Permissão `cloudwatch:PutMetricData` + `logs:CreateLogGroup` + `logs:PutLogEvents` na ECS Task Role para o sidecar publicar métricas

### Modified Capabilities

## Impact

- **`app/pom.xml`**: novas dependências Micrometer OTel registry e Quarkus OTel
- **`app/src/main/resources/application.properties`**: configuração OTLP endpoint e Micrometer
- **`terraform/modules/ecs/main.tf`**: sidecar container na Task Definition, nova IAM policy, log group para o collector
- **`terraform/modules/ecs/variables.tf`**: novas variáveis (ADOT image tag, métricas namespace, thresholds de alarme)
- **CloudWatch**: novo namespace de métricas `ticketmaster/app`, novo Dashboard, novos Alarmes
- **Custo estimado**: CloudWatch EMF é cobrado como Logs Ingestion (~$0.50/GB) + métricas derivadas via Metric Filters (mais barato que Custom Metrics direto)
