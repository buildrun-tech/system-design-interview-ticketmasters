## Why

O sistema não possui um ponto de entrada gerenciado e público para o microserviço — o NLB é interno e não expõe endpoints internet-facing. Um API Gateway HTTP API v2 resolve isso sem alterar a topologia interna, servindo como proxy transparente e ponto de controle futuro (throttling, autenticação, custom domain).

## What Changes

- **Novo módulo Terraform `terraform/modules/api-gateway`** contendo o HTTP API v2, VPC Link v2, integração HTTP_PROXY, rota catch-all `ANY /{proxy+}`, e stage `$default` com auto-deploy.
- **Remoção do `aws_api_gateway_vpc_link` do módulo `ecs`** — o VPC Link passa a pertencer ao módulo `api-gateway`, que é o consumidor natural do recurso.
- **Atualização do `security-group-chain`**: o NLB SG passa a aceitar tráfego apenas do SG do VPC Link v2 (não mais do CIDR da VPC), amarrando o acesso ao NLB exclusivamente ao API Gateway.
- **Novo Security Group `apigw-vpc-link-sg`** dentro do módulo `api-gateway`, anexado ao VPC Link v2, com egress permitido somente para o NLB SG.
- Módulo `api-gateway` é instanciado no `terraform/main.tf` recebendo `vpc_link_sg_id`, `nlb_arn`, `private_subnet_ids` e `vpc_id`.

## Capabilities

### New Capabilities

- `http-api-gateway`: API Gateway HTTP API v2 público (internet-facing), com rota `ANY /{proxy+}`, integração HTTP_PROXY via VPC Link para o NLB, stage `$default` com auto-deploy, e passagem transparente de paths, headers e query params.
- `apigw-vpc-link`: VPC Link v2 (`aws_apigatewayv2_vpc_link`) com Security Group próprio dentro do módulo `api-gateway`; SG configurado com egress restrito ao NLB SG, garantindo isolamento de rede entre API Gateway e NLB.

### Modified Capabilities

- `security-group-chain`: A regra de ingress do NLB SG muda de `cidr_blocks = [VPC CIDR]` para `security_groups = [apigw_vpc_link_sg_id]`, restringindo o acesso ao NLB exclusivamente ao VPC Link do API Gateway.
- `vpc-link-integration`: O VPC Link migra de REST API v1 (`aws_api_gateway_vpc_link`, no módulo `ecs`) para HTTP API v2 (`aws_apigatewayv2_vpc_link`, no módulo `api-gateway`).

## Impact

- **Terraform**: novo módulo `terraform/modules/api-gateway/`; `terraform/modules/ecs/main.tf` remove o `aws_api_gateway_vpc_link`; `terraform/modules/networking/main.tf` atualiza NLB SG; `terraform/main.tf` adiciona instância do módulo `api-gateway`.
- **Networking**: tráfego público chega via API Gateway HTTPS endpoint → VPC Link → NLB → ECS. O NLB deixa de aceitar qualquer tráfego do CIDR da VPC diretamente.
- **Sem breaking changes para a aplicação**: a app no ECS não muda — recebe requests exatamente como antes, com headers/query params preservados pelo proxy transparente.
- **Custom domain**: não incluído nesta mudança; será adicionado posteriormente como change separado.
