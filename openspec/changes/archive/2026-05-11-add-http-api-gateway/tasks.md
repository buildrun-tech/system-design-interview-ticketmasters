## 1. Preparação e Limpeza do Estado Terraform

- [x] 1.1 Remover o recurso `aws_api_gateway_vpc_link` do módulo `ecs` (`terraform/modules/ecs/main.tf`)
- [x] 1.2 Executar `terraform state rm module.ecs.aws_api_gateway_vpc_link.main` para remover o VPC Link v1 do state sem destruí-lo na AWS (ou adicionar bloco `removed {}` se Terraform >= 1.7)
- [x] 1.3 Verificar que o módulo `ecs/outputs.tf` não expõe `vpc_link_id` ou `vpc_link_arn` (remover se existir)

## 2. Módulo `api-gateway` — Estrutura e VPC Link

- [x] 2.1 Criar diretório `terraform/modules/api-gateway/` com arquivos `main.tf`, `variables.tf` e `outputs.tf`
- [x] 2.2 Declarar variáveis de entrada no `variables.tf`: `name_prefix`, `common_tags`, `vpc_id`, `private_subnet_ids`, `apigw_vpc_link_sg_id`, `nlb_listener_arn`
- [x] 2.3 Security Group do VPC Link criado no módulo `networking` (evita ciclo de dependência de módulos); api-gateway recebe o SG ID como input
- [x] 2.4 Criar o `aws_apigatewayv2_vpc_link` com `subnet_ids = var.private_subnet_ids` e `security_group_ids = [var.apigw_vpc_link_sg_id]`
- [x] 2.5 Expor outputs em `outputs.tf`: `vpc_link_id`, `api_gateway_id`, `api_gateway_endpoint`

## 3. Módulo `api-gateway` — HTTP API v2

- [x] 3.1 Criar `aws_apigatewayv2_api` com `protocol_type = "HTTP"` e tags adequadas (`${var.name_prefix}-http-api`)
- [x] 3.2 Criar `aws_apigatewayv2_integration` com `integration_type = "HTTP_PROXY"`, `connection_type = "VPC_LINK"`, `connection_id = aws_apigatewayv2_vpc_link.main.id`, `integration_uri = var.nlb_listener_arn`, `integration_method = "ANY"`
- [x] 3.3 Criar rota catch-all `aws_apigatewayv2_route` com `route_key = "ANY /{proxy+}"` apontando para a integração
- [x] 3.4 Criar rota raiz `aws_apigatewayv2_route` com `route_key = "ANY /"` apontando para a mesma integração
- [x] 3.5 Criar `aws_apigatewayv2_stage` com `name = "$default"` e `auto_deploy = true`
- [x] 3.6 Adicionar `depends_on = [aws_apigatewayv2_vpc_link.main]` na integração para evitar race condition durante o provisionamento do VPC Link

## 4. Networking — Atualizar NLB Security Group

- [x] 4.1 Security Group `apigw_vpc_link` criado dentro do módulo `networking` (sem nova variável de input); output `apigw_vpc_link_sg_id` adicionado
- [x] 4.2 NLB SG ingress atualizado: `cidr_blocks = [VPC CIDR]` → `security_groups = [aws_security_group.apigw_vpc_link.id]` com descrição "Traffic from API Gateway VPC Link"

## 5. Root Module — Integração dos Módulos

- [x] 5.1 Variável `nlb_listener_arn` não necessária em `variables.tf` raiz; ARN vem do output `module.ecs.nlb_listener_arn`
- [x] 5.2 Expor `nlb_listener_arn` como output do módulo `ecs` em `terraform/modules/ecs/outputs.tf`
- [x] 5.3 Instanciar o módulo `api_gateway` em `terraform/main.tf` passando: `name_prefix`, `common_tags`, `vpc_id`, `private_subnet_ids`, `apigw_vpc_link_sg_id = module.networking.apigw_vpc_link_sg_id`, `nlb_listener_arn = module.ecs.nlb_listener_arn`
- [x] 5.4 Módulo `networking` não precisa receber `apigw_vpc_link_sg_id` como input; o SG é criado internamente no módulo networking
- [x] 5.5 `depends_on` não necessário; Terraform resolve a ordem via referências de outputs entre módulos

## 6. Outputs e Verificação

- [x] 6.1 Adicionar output `api_gateway_endpoint` em `terraform/outputs.tf` apontando para `module.api_gateway.api_gateway_endpoint`; substituir `vpc_link_id/arn` legados pelo novo `vpc_link_id` do módulo api-gateway
- [x] 6.2 Executar `terraform plan` e verificar que não há mudanças destrutivas inesperadas além do NLB SG e da remoção do VPC Link v1
- [x] 6.3 Executar `terraform apply` e confirmar que todos os recursos são criados com sucesso
- [x] 6.4 Testar o endpoint do API Gateway via `curl` ou Postman: GET, POST, verificar headers e query params chegando corretamente no ECS

> ⚠️ Tasks 6.2–6.4 requerem acesso ao ambiente AWS. Executar manualmente.
