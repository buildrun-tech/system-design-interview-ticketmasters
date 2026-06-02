## ADDED Requirements

### Requirement: Policy CloudWatch na ECS Task Role para o ADOT Collector
A `ecs_task_role` SHALL ter uma policy IAM inline concedendo ao ADOT Collector (sidecar) as permissões mínimas para publicar métricas e logs no CloudWatch:
- `logs:CreateLogGroup`
- `logs:CreateLogStream`
- `logs:PutLogEvents`
- `logs:DescribeLogStreams`
- `cloudwatch:PutMetricData`

#### Scenario: Policy IAM criada e anexada à task role
- **WHEN** o Terraform é aplicado
- **THEN** a `ecs_task_role` possui a policy `<prefix>-ecs-task-cloudwatch-policy` com as actions listadas

#### Scenario: ADOT Collector consegue publicar métricas sem erro de permissão
- **WHEN** o container `aws-otel-collector` tenta exportar métricas via exporter `awsemf`
- **THEN** os logs do collector não contêm erros `AccessDenied` ou `UnauthorizedException`

---

### Requirement: Policy SSM na ECS Task Execution Role para ler config do Collector
A `ecs_task_execution_role` SHALL ter permissão para ler o SSM Parameter que contém a configuração do ADOT Collector:
- `ssm:GetParameter` no resource `arn:aws:ssm:<region>:<account>:parameter/ticketmaster/<env>/otel-collector-config`
- `kms:Decrypt` se o parameter for do tipo `SecureString` com KMS gerenciado pelo cliente (opcional — apenas se usar CMK)

#### Scenario: Task inicia com sucesso lendo o SSM Parameter do collector
- **WHEN** o ECS Agent resolve os secrets da Task Definition durante o startup
- **THEN** o container `aws-otel-collector` recebe a variável de ambiente `AOT_CONFIG_CONTENT` populada

#### Scenario: Erro de permissão SSM impede startup da task
- **WHEN** a task execution role não tem permissão `ssm:GetParameter` para o parameter do collector
- **THEN** a ECS Task falha em iniciar com o erro `ResourceInitializationError` nos logs do ECS Agent
