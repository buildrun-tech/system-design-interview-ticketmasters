## Context

O ECS service do Ticketmaster roda no Fargate com `desired_count` fixo gerenciado pelo Terraform. Container Insights já está habilitado no cluster, o que publica automaticamente a métrica `ECSServiceAverageCPUUtilization` no CloudWatch — pré-requisito para qualquer política de auto-scaling baseada em CPU.

O auto-scaling será implementado via **AWS Application Auto Scaling**, o serviço gerenciado da AWS que controla escalabilidade de recursos além de EC2 (incluindo ECS Fargate).

## Goals / Non-Goals

**Goals:**
- Escalar horizontalmente o ECS service quando a CPU média ultrapassa 70%
- Escalar para dentro automaticamente quando a carga diminui
- Manter entre 1 e 10 tasks em execução a qualquer momento
- Não quebrar o fluxo de CI/CD existente

**Non-Goals:**
- Scaling baseado em outras métricas (memória, SQS depth, latência)
- Scaling vertical (alterar CPU/memória da task definition)
- Scheduled scaling (horários fixos de expansão)
- Configuração de scale-in protection por task

## Decisions

### 1. Target Tracking em vez de Step Scaling

**Decisão:** usar `aws_appautoscaling_policy` com `policy_type = "TargetTrackingScaling"`.

**Rationale:** Target Tracking é análogo a um controlador PID — a AWS calcula quantas tasks adicionar/remover para manter a métrica no target, sem que precisemos definir manualmente cada degrau. Step Scaling exigiria calibrar manualmente thresholds de scale-in e scale-out, cooldowns separados e alarmes CloudWatch explícitos. Para o caso de uso atual (target único de 70%), Target Tracking é suficiente e mais simples de operar.

**Alternativa considerada:** Step Scaling — descartada por complexidade desnecessária dado que não há requisito de granularidade multi-threshold.

### 2. `ignore_changes` no `desired_count` do ECS service

**Decisão:** adicionar `lifecycle { ignore_changes = [desired_count] }` no `aws_ecs_service`.

**Rationale:** sem esse bloco, cada `terraform apply` resetaria o `desired_count` para o valor da variável (ex: 1), destruindo o estado gerenciado pelo auto-scaler. O Application Auto Scaling é a fonte de verdade para task count após o primeiro deploy; o Terraform só controla o valor inicial.

**Alternativa considerada:** remover a variável `desired_count` e deixar o auto-scaler sempre controlar. Descartada porque o valor inicial ainda é relevante em ambientes novos (ex: dev) antes do auto-scaler entrar em ação.

### 3. Métricas e cooldowns do Target Tracking

**Decisão:** usar a métrica predefinida `ECSServiceAverageCPUUtilization` com target de 70%. Cooldowns padrão da AWS para Target Tracking:
- Scale-out cooldown: **60s** (default do Target Tracking — configurável via `scale_out_cooldown`)
- Scale-in cooldown: **300s** (default — proteção contra thrashing)

Esses valores serão expostos como variáveis com defaults razoáveis.

### 4. Parametrização por variável no módulo ECS

**Decisão:** adicionar ao módulo `ecs` as variáveis:
- `autoscaling_min_capacity` (default: 1)
- `autoscaling_max_capacity` (default: 10)
- `autoscaling_cpu_target` (default: 70)
- `autoscaling_scale_in_cooldown` (default: 300)
- `autoscaling_scale_out_cooldown` (default: 60)

**Rationale:** permite que dev e prod usem valores diferentes sem duplicar recursos Terraform.

## Risks / Trade-offs

**Fargate Spot + auto-scaling** → Tasks Spot podem ser interrompidas pela AWS. O auto-scaler pode lançar novas tasks como reposição simultaneamente a um scale-out real por CPU. Isso é comportamento esperado e não causa problemas — o Application Auto Scaling apenas vê o task count resultante.

**Thrashing em cargas oscilatórias** → Se a carga flutua rapidamente em torno de 70%, tasks sobem e descem com frequência. Mitigação: o scale-in cooldown de 300s amortece esse comportamento.

**`desired_count` no CI/CD** → O GitHub Actions passa `desired_count` como input do Terraform. Após o `ignore_changes` ser aplicado, esse valor não terá efeito em applies subsequentes (exceto se o serviço for recriado). Não é necessário remover o input do CI/CD, mas o comportamento deve ser documentado.

**Tempo de warm-up de novas tasks** → O Fargate leva ~30-60s para inicializar uma nova task (pull de imagem + health check). Durante esse intervalo, a carga continua alta. O Target Tracking aguarda a task estabilizar antes de recalcular o scaling.

## Migration Plan

1. Adicionar recursos e variáveis ao módulo ECS (sem impacto no serviço existente até o apply)
2. Executar `terraform plan` para validar que apenas recursos de auto-scaling são criados (nenhum replace no ECS service)
3. Executar `terraform apply` — o `ignore_changes` passa a valer a partir deste apply
4. Verificar no console AWS que o scalable target foi registrado e a policy está ativa

**Rollback:** remover os recursos `aws_appautoscaling_target` e `aws_appautoscaling_policy` do Terraform e re-aplicar. O ECS service volta a operar com `desired_count` estático (removendo o `ignore_changes`).
