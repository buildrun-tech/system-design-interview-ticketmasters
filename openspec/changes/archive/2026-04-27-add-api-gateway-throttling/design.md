## Context

O projeto usa AWS API Gateway HTTP API v2 (`aws_apigatewayv2_api`) com um stage `$default` e auto-deploy habilitado. Atualmente o stage não possui nenhuma configuração de throttling, deixando o backend ECS exposto a qualquer volume de tráfego.

O throttling no HTTP API v2 é configurado no nível do stage via `default_route_settings`, que aplica limites a todas as rotas sem necessidade de configuração individual por rota.

## Goals / Non-Goals

**Goals:**
- Limitar todas as rotas a 5 requisições por segundo
- Burst limit = 5 (igual ao rate limit) para garantir zero tolerância a picos
- Requisições acima do limite recebem `429 Too Many Requests`

**Non-Goals:**
- Throttling por cliente/IP (requer WAF ou usage plans — não disponível no HTTP API v2 nativamente)
- Throttling diferenciado por rota
- Monitoramento ou alertas de throttling (fora do escopo desta change)

## Decisions

### Token Bucket com burst = rate

O API Gateway usa o algoritmo Token Bucket. Com `throttling_rate_limit = 5` e `throttling_burst_limit = 5`, o bucket nunca acumula tokens acima de 5, garantindo que mesmo após períodos de inatividade não seja possível enviar uma rajada maior que 5 req/s.

**Alternativas consideradas:**
- `burst = 10`: permitiria pequenos picos após inatividade — descartado porque o requisito é zero tolerância
- throttling por rota via `route_settings`: mais verboso e desnecessário dado que as rotas são catch-all

### Configuração no stage, não na API

O `default_route_settings` no `aws_apigatewayv2_stage` é o ponto correto para throttling global. Não existe bloco equivalente no recurso `aws_apigatewayv2_api`.

## Risks / Trade-offs

- **Health checks internos** → Se o load balancer ou outro serviço fizer polling frequente via API Gateway, pode atingir o limite. Mitigação: health checks devem acessar o NLB/ECS diretamente, não pelo API Gateway.
- **Terraform apply sem downtime** → A atualização do stage é in-place e não causa interrupção. O auto_deploy garante que a mudança entra em vigor imediatamente após o apply.
- **Limite muito restrito para produção** → 5 req/s é adequado para ambiente de estudo/desenvolvimento. Para produção real, revisar o valor conforme a carga esperada.
