## ADDED Requirements

### Requirement: Global throttling on all routes
O API Gateway HTTP API v2 SHALL limitar todas as rotas a no máximo 5 requisições por segundo com burst limit de 5, usando o algoritmo Token Bucket.

#### Scenario: Request within rate limit
- **WHEN** um cliente envia 5 ou menos requisições em 1 segundo
- **THEN** o API Gateway encaminha todas as requisições ao backend normalmente

#### Scenario: Request exceeds rate limit
- **WHEN** um cliente envia mais de 5 requisições em 1 segundo
- **THEN** o API Gateway retorna `429 Too Many Requests` para as requisições excedentes sem encaminhá-las ao backend

#### Scenario: No burst allowed after inactivity
- **WHEN** o sistema fica inativo por vários segundos e depois recebe 10 requisições simultâneas
- **THEN** apenas 5 requisições são processadas e as demais 5 recebem `429 Too Many Requests`

#### Scenario: Throttling applies to all routes
- **WHEN** requisições são feitas para qualquer rota (`ANY /` ou `ANY /{proxy+}`)
- **THEN** o mesmo limite de 5 req/s é aplicado a todas elas
