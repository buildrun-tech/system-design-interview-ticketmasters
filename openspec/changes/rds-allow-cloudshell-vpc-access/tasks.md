## 1. Networking Module — Data Source

- [x] 1.1 Adicionar `data "aws_security_group" "default"` em `terraform/modules/networking/main.tf` com `name = "default"` e `vpc_id = var.vpc_id`

## 2. RDS Security Group — Novas Regras Inbound

- [x] 2.1 Adicionar bloco `ingress` ao `aws_security_group.rds` para TCP 5432 via `data.aws_vpc.existing.cidr_block` (description: "PostgreSQL from VPC CIDR")
- [x] 2.2 Adicionar bloco `ingress` ao `aws_security_group.rds` para TCP 5432 via `data.aws_security_group.default.id` (description: "PostgreSQL from default SG")

## 3. Validação

- [x] 3.1 Executar `terraform plan` e confirmar que apenas os dois `ingress` são adicionados ao SG do RDS, sem remoção de regras existentes
- [x] 3.2 Executar `terraform apply` e verificar no console AWS que as três regras inbound aparecem no SG do RDS
- [x] 3.3 Abrir CloudShell VPC na subnet correta e confirmar conexão com `psql -h <rds-endpoint> -U <user> -d <db>`
