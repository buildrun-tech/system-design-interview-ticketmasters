## 1. Instrumentação da Aplicação Quarkus

- [x] 1.1 Adicionar dependências `quarkus-micrometer`, `quarkus-opentelemetry` e o bridge OTLP no `app/pom.xml`
- [x] 1.2 Configurar o exporter OTLP em `application.properties`: endpoint `http://localhost:4317`, intervalo 60s, namespace `TicketMaster/App`
- [ ] 1.3 Validar que `/q/metrics` retorna métricas HTTP (`http_server_requests_seconds`), JVM (`jvm_memory_used`) e DB pool (`agroal_connections`) localmente — requer `mvn quarkus:dev` com DB local
- [ ] 1.4 Validar que a aplicação sobe normalmente quando o collector não está disponível (falha silenciosa no export) — requer ambiente local ou dev

## 2. Configuração do ADOT Collector

- [x] 2.1 Criar o YAML de configuração do ADOT Collector com pipeline `otlp → batch → awsemf` (namespace `TicketMaster/App`, dimensões `ServiceName` e `Environment`)
- [x] 2.2 Criar o SSM Parameter `/ticketmaster/<env>/otel-collector-config` no Terraform (tipo `String`, valor = YAML de configuração)

## 3. Infra ECS — Task Definition e IAM

- [x] 3.1 Adicionar variáveis Terraform para: `adot_image_tag`, `otel_collector_ssm_param_arn`, alarme thresholds (`latency_p99_threshold_ms`, `error_rate_threshold_percent`)
- [x] 3.2 Adicionar o container sidecar `aws-otel-collector` na Task Definition: `essential: false`, 256 CPU units, 256 MB, secret `AOT_CONFIG_CONTENT` do SSM, logDriver `awslogs` apontando para o log group dedicado
- [x] 3.3 Criar CloudWatch Log Group `/ecs/ticketmaster-otel-collector` via Terraform com a mesma retention do log group da app
- [x] 3.4 Adicionar policy IAM inline `ecs-task-cloudwatch-policy` na `ecs_task_role` com `cloudwatch:PutMetricData` + `logs:*` necessários para o EMF exporter
- [x] 3.5 Adicionar permissão `ssm:GetParameter` para o parameter do collector na `ecs_task_execution_role`

## 4. CloudWatch Dashboard e Alarmes

- [x] 4.1 Criar o CloudWatch Dashboard `ticketmaster-golden-signals-<env>` via Terraform com 4 widgets: Latência (p50/p95/p99), Tráfego (req/s), Erros (4xx e 5xx rate), Saturação (JVM heap %, DB pool connections)
- [x] 4.2 Criar CloudWatch Alarm para Latência p99 alta: threshold configurável, 2 evaluation periods de 60s
- [x] 4.3 Criar CloudWatch Alarm para Error Rate 5xx: threshold configurável, 2 evaluation periods de 60s

## 5. Validação End-to-End

- [ ] 5.1 Aplicar Terraform e verificar que a ECS Task sobe com ambos os containers (app + collector)
- [ ] 5.2 Verificar que o namespace `TicketMaster/App` aparece no CloudWatch Metrics console com as métricas dos 4 golden signals
- [ ] 5.3 Verificar que o Dashboard exibe dados após gerar tráfego de teste via collection Bruno
- [ ] 5.4 Simular erro 5xx e verificar que o Alarm de error rate transita para `ALARM`
