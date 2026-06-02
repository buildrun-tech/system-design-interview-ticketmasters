## ADDED Requirements

### Requirement: Container ADOT Collector na ECS Task Definition
A Task Definition SHALL incluir um segundo container usando a imagem `amazon/aws-otel-collector` como sidecar, com as seguintes características:
- Nome: `aws-otel-collector`
- Porta: não exposta externamente (comunicação interna via loopback)
- `essential: false` — falha do collector não deve derrubar o container principal
- CPU: 256 units | Memória: 256 MB

#### Scenario: Task Definition com dois containers
- **WHEN** a Task Definition é aplicada via Terraform
- **THEN** a ECS Task sobe com dois containers: `ticketmaster-app` e `aws-otel-collector`

#### Scenario: Falha do sidecar não afeta aplicação principal
- **WHEN** o container `aws-otel-collector` para inesperadamente
- **THEN** o container `ticketmaster-app` continua servindo requisições (o sidecar é `essential: false`)

---

### Requirement: Configuração do ADOT Collector via SSM Parameter Store
A configuração YAML do ADOT Collector SHALL ser armazenada no AWS SSM Parameter Store (tipo `SecureString`) e injetada no container via variável de ambiente `AOT_CONFIG_CONTENT`, referenciada como `secret` na Task Definition.

#### Scenario: SSM Parameter criado via Terraform
- **WHEN** o Terraform é aplicado
- **THEN** um SSM Parameter `/ticketmaster/<env>/otel-collector-config` existe no Parameter Store com o YAML de configuração

#### Scenario: Collector inicia com configuração do SSM
- **WHEN** o container `aws-otel-collector` inicia
- **THEN** logs do container mostram o pipeline `otlp → batch → awsemf` inicializado

---

### Requirement: Pipeline OTLP → EMF no ADOT Collector
O ADOT Collector SHALL ser configurado com o seguinte pipeline de métricas:
- **Receiver**: `otlp` em `0.0.0.0:4317` (gRPC)
- **Processor**: `batch` com timeout de 60s e `send_batch_max_size: 1000`
- **Exporter**: `awsemf` com namespace `TicketMaster/App`, dimensões `[ServiceName, Environment]`

#### Scenario: Métricas recebidas via OTLP e publicadas no CloudWatch
- **WHEN** a aplicação envia métricas OTLP para `localhost:4317`
- **THEN** dentro de 90 segundos as métricas aparecem no namespace `TicketMaster/App` no CloudWatch

#### Scenario: Logs EMF escritos no Log Group correto
- **WHEN** o exporter EMF publica métricas
- **THEN** os logs EMF são escritos no Log Group `/ecs/ticketmaster-otel-collector` na região configurada

---

### Requirement: Log Group dedicado para o container ADOT Collector
Um CloudWatch Log Group SHALL ser criado via Terraform para os logs do container sidecar, separado do log group da aplicação principal.

#### Scenario: Log Group do collector criado com retenção configurada
- **WHEN** o Terraform é aplicado
- **THEN** o Log Group `/ecs/ticketmaster-otel-collector` existe com retention igual ao log group da aplicação
