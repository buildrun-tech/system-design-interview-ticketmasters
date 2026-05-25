## Why

O ECS service atualmente opera com `desired_count` estático, sem capacidade de responder automaticamente a variações de carga. Com picos de tráfego (ex: abertura de vendas de ingressos), as tasks existentes ficam saturadas sem qualquer mecanismo de expansão — degradando latência e podendo causar falhas. O auto-scaling com Target Tracking resolve isso de forma simples e gerenciada pela AWS.

## What Changes

- Registrar o ECS service como um **scalable target** no Application Auto Scaling
- Criar uma **Target Tracking Scaling Policy** baseada na métrica `ECSServiceAverageCPUUtilization` com target de 70%
- Definir limites de capacidade: mínimo de 1 task, máximo de 10 tasks
- Expor variáveis no módulo ECS (`min_capacity`, `max_capacity`, `cpu_target_value`) para parametrização por ambiente
- Ajustar o `desired_count` do `aws_ecs_service` para ser ignorado após o auto-scaling assumir o controle (`ignore_changes`)

## Capabilities

### New Capabilities

- `ecs-cpu-autoscaling`: Escalonamento horizontal automático do ECS service baseado em CPU média, via Application Auto Scaling com Target Tracking Policy.

### Modified Capabilities

- `ecs-service-task-sync`: O número de tasks passa a ser gerenciado pelo auto-scaler; deployments de CI/CD não devem mais forçar `desired_count` como valor definitivo.

## Impact

- **Terraform**: módulo `terraform/modules/ecs/` — novos recursos `aws_appautoscaling_target` e `aws_appautoscaling_policy`; novas variáveis em `variables.tf`; novo output em `outputs.tf`
- **`terraform/main.tf`**: passagem das novas variáveis (`min_capacity`, `max_capacity`, `cpu_target_value`) para o módulo ECS
- **`terraform/variables.tf`**: declaração das novas variáveis raiz
- **CI/CD**: nenhuma mudança necessária — o auto-scaling não interfere no fluxo de deploy
- **Pré-requisito satisfeito**: Container Insights já está habilitado, fornecendo a métrica `ECSServiceAverageCPUUtilization` ao CloudWatch sem custo adicional
