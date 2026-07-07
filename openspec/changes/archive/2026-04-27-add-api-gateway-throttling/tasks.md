## 1. Terraform - API Gateway Module

- [x] 1.1 Adicionar bloco `default_route_settings` no recurso `aws_apigatewayv2_stage.default` em `terraform/modules/api-gateway/main.tf` com `throttling_rate_limit = 5` e `throttling_burst_limit = 5`

## 2. Validação

- [x] 2.1 Executar `terraform plan` e verificar que apenas o `aws_apigatewayv2_stage.default` aparece como modificado (in-place update)
- [x] 2.2 Executar `terraform apply` e confirmar que o stage é atualizado sem erros
- [x] 2.3 Testar o throttling enviando mais de 5 requisições em 1 segundo e verificar resposta `429 Too Many Requests` nas excedentes
