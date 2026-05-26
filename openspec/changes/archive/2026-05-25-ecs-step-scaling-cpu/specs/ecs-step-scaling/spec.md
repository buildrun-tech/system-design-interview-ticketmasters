## ADDED Requirements

### Requirement: Scale-out alarm com alta resolução
O sistema SHALL criar um CloudWatch Alarm para scale-out com `period = 30`, `evaluation_periods = 1`, `statistic = Average` e `comparison_operator = GreaterThanOrEqualToThreshold` sobre a métrica `ECSServiceAverageCPUUtilization`.

#### Scenario: Alarme dispara com CPU elevada por 30 segundos
- **WHEN** a CPU média do serviço ECS excede o threshold de scale-out por 1 período de 30s
- **THEN** o alarme transita para estado ALARM e aciona a política de step scaling de scale-out

#### Scenario: Alarme não dispara por spike momentâneo
- **WHEN** a CPU sobe abruptamente mas a média do período de 30s fica abaixo do threshold
- **THEN** o alarme permanece em OK e nenhuma ação de scaling é executada

---

### Requirement: Política de step scaling de scale-out com 3 steps
O sistema SHALL criar uma política StepScaling de scale-out com três steps progressivos baseados no desvio de CPU acima do threshold do alarme.

#### Scenario: Pressão moderada de CPU
- **WHEN** o alarme de scale-out está em ALARM e o desvio de CPU é entre 0% e 10% acima do threshold
- **THEN** o sistema adiciona exatamente 1 task ao serviço ECS

#### Scenario: Pressão alta de CPU
- **WHEN** o alarme de scale-out está em ALARM e o desvio de CPU é entre 10% e 25% acima do threshold
- **THEN** o sistema adiciona exatamente 2 tasks ao serviço ECS

#### Scenario: Pressão crítica de CPU
- **WHEN** o alarme de scale-out está em ALARM e o desvio de CPU é 25% ou mais acima do threshold
- **THEN** o sistema adiciona exatamente 4 tasks ao serviço ECS

#### Scenario: Capacidade máxima atingida
- **WHEN** o scaling seria aplicado mas o número de tasks já está em `autoscaling_max_capacity`
- **THEN** nenhuma task adicional é criada e o alarme permanece em ALARM

---

### Requirement: Alarme de scale-in conservador
O sistema SHALL criar um CloudWatch Alarm para scale-in com `period = 60`, `evaluation_periods = 3`, `statistic = Average` e `comparison_operator = LessThanThreshold`.

#### Scenario: Alarme dispara após CPU baixa sustentada
- **WHEN** a CPU média do serviço ECS fica abaixo do threshold de scale-in por 3 períodos consecutivos de 60s
- **THEN** o alarme transita para estado ALARM e aciona a política de step scaling de scale-in

#### Scenario: Alarme não dispara por queda temporária de CPU
- **WHEN** a CPU cai abaixo do threshold mas sobe novamente dentro dos 3 períodos de avaliação
- **THEN** o alarme permanece em OK e nenhum scale-in é executado

---

### Requirement: Política de step scaling de scale-in
O sistema SHALL criar uma política StepScaling de scale-in que remove 1 task para qualquer desvio abaixo do threshold do alarme de scale-in.

#### Scenario: CPU consistentemente baixa
- **WHEN** o alarme de scale-in está em ALARM
- **THEN** o sistema remove exatamente 1 task do serviço ECS

#### Scenario: Capacidade mínima atingida
- **WHEN** o scale-in seria aplicado mas o número de tasks já está em `autoscaling_min_capacity`
- **THEN** nenhuma task é removida

---

### Requirement: Thresholds configuráveis via variáveis Terraform
O módulo ECS SHALL expor variáveis para os thresholds de CPU de scale-out e scale-in, substituindo a variável `autoscaling_cpu_target` do Target Tracking.

#### Scenario: Threshold de scale-out configurado pelo chamador
- **WHEN** o módulo é invocado com `autoscaling_scale_out_cpu_threshold = 70`
- **THEN** o CloudWatch Alarm de scale-out usa 70% como threshold

#### Scenario: Threshold de scale-in configurado pelo chamador
- **WHEN** o módulo é invocado com `autoscaling_scale_in_cpu_threshold = 30`
- **THEN** o CloudWatch Alarm de scale-in usa 30% como threshold
