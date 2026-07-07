## Context

O módulo ECS (`terraform/modules/ecs/`) usa atualmente `TargetTrackingScaling` para CPU. Essa abordagem delega o controle de scaling à AWS, que usa uma avaliação interna com períodos de 60s e múltiplos datapoints — resultando em latência de reação de 3-5 minutos. Para um sistema de venda de ingressos, picos de tráfego são abruptos e curtos; escalar tarde significa requests perdidos ou degradação perceptível.

A métrica `ECSServiceAverageCPUUtilization` é a métrica predefinida disponível no Target Tracking. No Step Scaling, usaremos a mesma métrica via CloudWatch Alarm customizado com **high-resolution period de 30s** e `evaluation_periods = 1`.

## Goals / Non-Goals

**Goals:**
- Reduzir latência de reação ao scaling de ~3-5 min para ~30 segundos
- Implementar steps progressivos de scale-out (proporcional à gravidade do pico)
- Manter scale-in conservador para evitar thrashing
- Expor todos os thresholds como variáveis Terraform configuráveis

**Non-Goals:**
- Mudar a métrica base (continua sendo CPU)
- Adicionar scaling baseado em SQS ou métricas customizadas
- Alterar limites de min/max capacity existentes

## Decisions

### D1: Step Scaling sobre Target Tracking

**Decisão:** Substituir completamente o Target Tracking pelo Step Scaling.

**Por quê:** Target Tracking e Step Scaling não coexistem bem na mesma dimensão — a AWS pode sobrepor ações e criar comportamento imprevisível. Ao substituir completamente, o controle fica totalmente explícito.

**Alternativa considerada:** Manter Target Tracking como base e adicionar alarmes de step para situações extremas (CPU > 90%). Descartado porque manteria a latência lenta no caminho principal.

---

### D2: Alarme de scale-out com period=30s e evaluation_periods=1

**Decisão:** CloudWatch Alarm com `period = 30` e `evaluation_periods = 1`.

**Por quê:** Um único datapoint de 30s é suficiente para confirmar um pico real de CPU. Com `statistic = Average`, um spike momentâneo de processo único não vai inflar a média do cluster o suficiente para disparar falsos alarmes.

**Alternativa considerada:** `evaluation_periods = 2` (60s total). Mais seguro contra ruído, mas dobra a latência de reação — descartado dado o objetivo.

**Nota:** High-resolution metrics (period < 60s) têm custo adicional no CloudWatch ($0.30/métrica/mês).

---

### D3: Steps progressivos para scale-out

**Decisão:** Três steps de scale-out baseados em desvio acima do threshold:

```
Threshold (alarm): CPU > 60%

Step 1 — CPU 60-70%  → +1 task  (pressão moderada)
Step 2 — CPU 70-85%  → +2 tasks (pressão alta)
Step 3 — CPU 85%+    → +4 tasks (emergência)
```

Os steps são definidos como `metric_interval_lower_bound` / `metric_interval_upper_bound` relativos ao threshold do alarme. Ex: se threshold = 60, o step 2 começa em `lower_bound = 10` (60+10 = 70%).

**Por quê:** Scaling proporcional evita adicionar tasks desnecessários em picos pequenos e garante resposta agressiva em picos sérios.

---

### D4: Scale-in conservador

**Decisão:** Um único step de scale-in com `evaluation_periods = 3` e `period = 60s` (3 minutos de CPU baixa confirmada antes de remover tasks).

**Por quê:** Scale-in agressivo cria thrashing — remover tasks quando CPU ainda está oscilando pode causar novo scale-out imediato. Scale-in lento é a troca aceitável.

```
Alarm scale-in: CPU < 40%  (evaluation_periods=3, period=60s)
Step: qualquer desvio abaixo do threshold → -1 task
Cooldown scale-in: 300s
```

---

### D5: Remover variável `autoscaling_cpu_target`

**Decisão:** Remover `autoscaling_cpu_target` (usada apenas pelo Target Tracking) e substituir por variáveis explícitas de threshold.

**Variáveis novas:**
- `autoscaling_scale_out_cpu_threshold` (default: 60) — CPU % que dispara scale-out alarm
- `autoscaling_scale_in_cpu_threshold` (default: 40) — CPU % que dispara scale-in alarm

## Risks / Trade-offs

| Risco | Mitigação |
|---|---|
| Falsos positivos com period=30s causam scale-out desnecessário | `statistic = Average` dilui spikes pontuais; cooldown de 60s evita cascata |
| Custo adicional de high-resolution metrics | Mínimo (~$0.30/mês); justificado pela latência de reação |
| Remoção de `autoscaling_cpu_target` é breaking change no módulo | Documentar na PR; o `main.tf` raiz precisa ser atualizado simultaneamente |
| Scale-in lento pode manter tasks ociosas por mais tempo | Trade-off intencional — custo de tasks extras é menor que custo de degradação |

## Migration Plan

1. Aplicar `terraform plan` para validar diff antes de `apply`
2. O `apply` remove o Target Tracking e cria os recursos de Step Scaling atomicamente
3. Rollback: reverter os arquivos `.tf` e reaplicar — o Target Tracking é recriado em segundos
4. Não há migração de dados ou downtime de aplicação
