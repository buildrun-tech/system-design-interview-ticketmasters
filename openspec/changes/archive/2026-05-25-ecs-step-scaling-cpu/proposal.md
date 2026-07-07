## Why

O ECS utiliza atualmente Target Tracking Scaling baseado em CPU, que delega o controle de scaling inteiramente para a AWS e reage com latência de 3-5 minutos. Para um sistema de venda de ingressos com picos abruptos de tráfego, essa latência é inaceitável — precisamos escalar em ~30 segundos após o pico começar.

## What Changes

- **Remover** a política `TargetTrackingScaling` de CPU (`aws_appautoscaling_policy.ecs_cpu`)
- **Adicionar** CloudWatch Alarm de scale-out com métrica de alta resolução (30s) e `evaluation_periods = 1`
- **Adicionar** CloudWatch Alarm de scale-in com métrica padrão e avaliação conservadora
- **Adicionar** política `StepScaling` de scale-out com 3 steps progressivos de CPU
- **Adicionar** política `StepScaling` de scale-in com step conservador
- **Atualizar** variáveis do módulo ECS para expor os thresholds de cada step

## Capabilities

### New Capabilities

- `ecs-step-scaling`: Política de auto-scaling baseada em steps de CPU com alarmes CloudWatch de alta resolução (30s) e disparo em 1 período de avaliação

### Modified Capabilities

_(nenhuma — a mudança é apenas na camada de implementação do scaling, sem alteração de requisitos de negócio)_

## Impact

- `terraform/modules/ecs/main.tf`: remoção do recurso `aws_appautoscaling_policy.ecs_cpu`, adição de 2 alarmes CloudWatch + 2 políticas Step Scaling
- `terraform/modules/ecs/variables.tf`: novas variáveis para thresholds de CPU dos steps
- `terraform/main.tf`: passagem das novas variáveis ao módulo ECS
- Nenhuma mudança em código de aplicação ou APIs
