## Why

O acesso direto ao banco de dados RDS é necessário para operações de depuração e administração, mas atualmente o security group do RDS só permite conexões vindas das tasks ECS. O CloudShell VPC oferece uma forma segura de acessar recursos privados da VPC sem expor o banco publicamente — habilitar esse acesso resolve a necessidade de inspeção e manutenção do banco de dados em ambiente de desenvolvimento.

## What Changes

- Adicionar regra inbound ao security group do RDS para permitir acesso via CIDR da VPC (172.31.0.0/16), cobrindo conexões originadas pelo CloudShell VPC
- Adicionar regra inbound ao security group do RDS para permitir acesso do security group `default` da VPC, que é associado às ENIs criadas pelo CloudShell VPC

## Capabilities

### New Capabilities

- `rds-cloudshell-vpc-access`: Regras de inbound no security group do RDS que permitem conexão PostgreSQL (porta 5432) via CloudShell VPC, usando o CIDR da VPC e o security group default como fontes autorizadas

### Modified Capabilities

- `security-group-chain`: O security group do RDS recebe novas regras inbound além da já existente (acesso via ECS tasks)

## Impact

- `terraform/modules/networking/main.tf`: adição de dois blocos `ingress` ao recurso `aws_security_group.rds`
- Nenhuma mudança em aplicação, API ou dependências de runtime
- Sem breaking changes — as regras existentes são mantidas
