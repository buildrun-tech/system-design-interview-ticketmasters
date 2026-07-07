## 1. Variáveis do módulo ECS

- [x] 1.1 Remover variável `autoscaling_cpu_target` de `terraform/modules/ecs/variables.tf`
- [x] 1.2 Adicionar variável `autoscaling_scale_out_cpu_threshold` (type=number, default=60) em `variables.tf`
- [x] 1.3 Adicionar variável `autoscaling_scale_in_cpu_threshold` (type=number, default=40) em `variables.tf`

## 2. Remoção do Target Tracking

- [x] 2.1 Remover recurso `aws_appautoscaling_policy.ecs_cpu` (TargetTrackingScaling) de `terraform/modules/ecs/main.tf`

## 3. CloudWatch Alarms

- [x] 3.1 Adicionar `aws_cloudwatch_metric_alarm.ecs_cpu_scale_out` em `main.tf` com `period=30`, `evaluation_periods=1`, `statistic="Average"`, `comparison_operator="GreaterThanOrEqualToThreshold"`, threshold=`var.autoscaling_scale_out_cpu_threshold`
- [x] 3.2 Adicionar `aws_cloudwatch_metric_alarm.ecs_cpu_scale_in` em `main.tf` com `period=60`, `evaluation_periods=3`, `statistic="Average"`, `comparison_operator="LessThanThreshold"`, threshold=`var.autoscaling_scale_in_cpu_threshold`
- [x] 3.3 Vincular os alarmes às políticas de scaling via `alarm_actions`

## 4. Políticas de Step Scaling

- [x] 4.1 Adicionar `aws_appautoscaling_policy.ecs_step_scale_out` com `policy_type="StepScaling"`, `adjustment_type="ChangeInCapacity"`, cooldown=`var.autoscaling_scale_out_cooldown` e 3 step adjustments:
  - `[0, 10)` → `+1`
  - `[10, 25)` → `+2`
  - `[25, null)` → `+4`
- [x] 4.2 Adicionar `aws_appautoscaling_policy.ecs_step_scale_in` com `policy_type="StepScaling"`, `adjustment_type="ChangeInCapacity"`, cooldown=`var.autoscaling_scale_in_cooldown` e 1 step adjustment:
  - `(null, 0]` → `-1`

## 5. Atualização do módulo raiz

- [x] 5.1 Remover a passagem de `autoscaling_cpu_target` ao módulo ECS em `terraform/main.tf`
- [x] 5.2 Adicionar passagem de `autoscaling_scale_out_cpu_threshold` e `autoscaling_scale_in_cpu_threshold` ao módulo ECS (ou deixar defaults ativos)

## 6. Validação

- [x] 6.1 Executar `terraform validate` e confirmar sem erros
- [x] 6.2 Executar `terraform plan` e revisar o diff: 1 recurso removido, 4 recursos adicionados (2 alarms + 2 policies)
