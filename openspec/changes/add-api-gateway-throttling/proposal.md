## Why

O API Gateway está exposto sem nenhum controle de taxa, tornando o sistema vulnerável a picos de tráfego e abusos que podem sobrecarregar o ECS downstream. Aplicar throttling garante previsibilidade e proteção do backend.

## What Changes

- Adicionar bloco `default_route_settings` no `aws_apigatewayv2_stage` com throttling global de 5 req/s
- O burst limit será igual ao rate limit (5), garantindo zero tolerância a picos — todas as requisições acima de 5/s recebem HTTP 429
- A configuração se aplica automaticamente a todas as rotas existentes (`ANY /` e `ANY /{proxy+}`) e a qualquer rota futura

## Capabilities

### New Capabilities

- `api-gateway-throttling`: Controle de taxa global no API Gateway HTTP API v2, limitando todas as rotas a 5 requisições por segundo com burst = 5

### Modified Capabilities

## Impact

- **Terraform**: `terraform/modules/api-gateway/main.tf` — adição do bloco `default_route_settings` no `aws_apigatewayv2_stage.default`
- **Comportamento**: Requisições que excedam 5/s receberão `429 Too Many Requests` imediatamente, sem burst
- **Sem impacto em rotas individuais**: nenhum `route_settings` por rota é necessário, pois o catch-all já cobre tudo
