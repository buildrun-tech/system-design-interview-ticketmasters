## 1. Variáveis do módulo ECS

- [x] 1.1 Adicionar variável `autoscaling_min_capacity` (type: number, default: 1) em `terraform/modules/ecs/variables.tf`
- [x] 1.2 Adicionar variável `autoscaling_max_capacity` (type: number, default: 10) em `terraform/modules/ecs/variables.tf`
- [x] 1.3 Adicionar variável `autoscaling_cpu_target` (type: number, default: 70) em `terraform/modules/ecs/variables.tf`
- [x] 1.4 Adicionar variável `autoscaling_scale_in_cooldown` (type: number, default: 300) em `terraform/modules/ecs/variables.tf`
- [x] 1.5 Adicionar variável `autoscaling_scale_out_cooldown` (type: number, default: 60) em `terraform/modules/ecs/variables.tf`

## 2. Recursos de auto-scaling no módulo ECS

- [x] 2.1 Adicionar `aws_appautoscaling_target` em `terraform/modules/ecs/main.tf` referenciando o `aws_ecs_service.app` com `min_capacity` e `max_capacity`
- [x] 2.2 Adicionar `aws_appautoscaling_policy` em `terraform/modules/ecs/main.tf` com `policy_type = "TargetTrackingScaling"`, métrica `ECSServiceAverageCPUUtilization` e `target_value` parametrizado
- [x] 2.3 Configurar `scale_in_cooldown` e `scale_out_cooldown` na policy usando as variáveis do passo 1

## 3. Ajuste do lifecycle do ECS service

- [x] 3.1 Adicionar bloco `lifecycle { ignore_changes = [desired_count] }` no `aws_ecs_service.app` em `terraform/modules/ecs/main.tf`

## 4. Variáveis raiz e passagem ao módulo

- [x] 4.1 Declarar variáveis `ecs_autoscaling_min_capacity`, `ecs_autoscaling_max_capacity`, `ecs_autoscaling_cpu_target` em `terraform/variables.tf`
- [x] 4.2 Passar as novas variáveis para o módulo ECS em `terraform/main.tf`

## 5. Outputs do módulo ECS

- [x] 5.1 Adicionar output `autoscaling_policy_arn` em `terraform/modules/ecs/outputs.tf`

