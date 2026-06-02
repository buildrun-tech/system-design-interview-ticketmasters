## ADDED Requirements

### Requirement: Dashboard CloudWatch com os 4 Golden Signals
Um CloudWatch Dashboard SHALL ser criado via Terraform chamado `ticketmaster-golden-signals-<environment>` com widgets para cada um dos 4 sinais:
1. **Latência**: p50, p95, p99 de `http_server_requests_seconds` (histograma)
2. **Tráfego**: requests/segundo (rate do counter `http_server_requests_seconds_count`)
3. **Erros**: taxa de erros 5xx e 4xx em percentual do total de requests
4. **Saturação**: JVM heap utilization (%) e DB pool active connections

#### Scenario: Dashboard criado via Terraform
- **WHEN** o Terraform é aplicado
- **THEN** o Dashboard `ticketmaster-golden-signals-<env>` existe no CloudWatch console

#### Scenario: Widgets do dashboard populados com dados
- **WHEN** a aplicação está rodando e recebendo tráfego por pelo menos 5 minutos
- **THEN** todos os 4 widgets do dashboard exibem séries temporais com dados

---

### Requirement: Alarme CloudWatch para Latência Alta (p99)
Um CloudWatch Alarm SHALL ser criado para o percentil 99 de latência HTTP com threshold configurável via variável Terraform.

#### Scenario: Alarme dispara quando p99 excede threshold
- **WHEN** o percentil 99 de latência HTTP permanece acima do threshold por 2 períodos consecutivos de 60 segundos
- **THEN** o alarme transita para estado `ALARM`

#### Scenario: Alarme retorna ao OK após melhora de latência
- **WHEN** o percentil 99 de latência cai abaixo do threshold por 3 períodos consecutivos
- **THEN** o alarme transita para estado `OK`

---

### Requirement: Alarme CloudWatch para Taxa de Erros HTTP
Um CloudWatch Alarm SHALL ser criado para a taxa de respostas HTTP 5xx, com threshold configurável via variável Terraform.

#### Scenario: Alarme dispara quando error rate excede threshold
- **WHEN** a percentagem de respostas 5xx ultrapassa o threshold por 2 períodos consecutivos de 60 segundos
- **THEN** o alarme transita para estado `ALARM`

#### Scenario: Sem alarme para erros 4xx isolados
- **WHEN** apenas respostas 4xx são geradas (sem 5xx)
- **THEN** o alarme de error rate permanece em `OK` (4xx são erros de cliente, não de serviço)
