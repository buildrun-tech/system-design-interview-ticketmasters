# ecs-cpu-autoscaling Specification

## Purpose
Define horizontal auto-scaling for ECS services based on CPU utilization using AWS Application Auto Scaling with Target Tracking Policy, ensuring tasks scale out and in automatically to maintain CPU usage near the configured target.

## Requirements

### Requirement: ECS service registrado como scalable target
O sistema SHALL registrar o ECS service no Application Auto Scaling com limites mínimo e máximo de capacidade configuráveis.

#### Scenario: Scalable target criado pelo Terraform
- **WHEN** o Terraform é aplicado com auto-scaling habilitado
- **THEN** existe um `aws_appautoscaling_target` referenciando o ECS service com `min_capacity = 1` e `max_capacity = 10`

#### Scenario: Task count não ultrapassa os limites definidos
- **WHEN** o auto-scaler tenta escalar além do `max_capacity`
- **THEN** o número de tasks permanece em no máximo 10

#### Scenario: Task count não cai abaixo do mínimo
- **WHEN** o auto-scaler tenta reduzir abaixo do `min_capacity`
- **THEN** o número de tasks permanece em no mínimo 1

### Requirement: Scale out automático por CPU média acima do target
O sistema SHALL aumentar o número de tasks quando a CPU média do serviço ultrapassar 70%.

#### Scenario: CPU média excede o target
- **WHEN** a métrica `ECSServiceAverageCPUUtilization` do CloudWatch supera 70% por um período sustentado
- **THEN** o Application Auto Scaling lança tasks adicionais até que a CPU média retorne ao target

#### Scenario: Scale out respeitando cooldown
- **WHEN** um scale-out é concluído
- **THEN** nenhum novo scale-out é iniciado antes de decorrido o `scale_out_cooldown` (default: 60s)

### Requirement: Scale in automático por CPU média abaixo do target
O sistema SHALL reduzir o número de tasks quando a CPU média do serviço ficar consistentemente abaixo de 70%.

#### Scenario: CPU média cai abaixo do target
- **WHEN** a métrica `ECSServiceAverageCPUUtilization` fica abaixo de 70% por um período sustentado
- **THEN** o Application Auto Scaling termina tasks excedentes até que a CPU média retorne próxima ao target

#### Scenario: Scale in respeitando cooldown
- **WHEN** um scale-in é concluído
- **THEN** nenhum novo scale-in é iniciado antes de decorrido o `scale_in_cooldown` (default: 300s)

### Requirement: Target Tracking Policy baseada em CPU predefinida da AWS
O sistema SHALL usar a métrica predefinida `ECSServiceAverageCPUUtilization` na Target Tracking Policy, sem alarmes CloudWatch customizados.

#### Scenario: Policy configurada com métrica predefinida
- **WHEN** o Terraform é aplicado
- **THEN** a `aws_appautoscaling_policy` referencia `predefined_metric_type = "ECSServiceAverageCPUUtilization"` com `target_value = 70`
