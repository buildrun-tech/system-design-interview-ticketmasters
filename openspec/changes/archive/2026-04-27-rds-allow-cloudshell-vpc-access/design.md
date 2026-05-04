## Context

O RDS PostgreSQL está em subnets privadas e seu security group (`aws_security_group.rds`) atualmente aceita conexões inbound somente do security group das tasks ECS. Isso impede acesso direto ao banco por ferramentas de administração como o CloudShell VPC.

O AWS CloudShell com acesso à VPC cria ENIs nas subnets configuradas e associa o security group **default** da VPC a essas interfaces. O IP das ENIs pertence ao CIDR da VPC (`172.31.0.0/16`). Para autorizar o CloudShell VPC, portanto, são necessárias duas regras: uma baseada no CIDR da VPC e outra referenciando o SG default.

O data source `data.aws_vpc.existing` já existe em `terraform/modules/networking/main.tf`, então o CIDR pode ser lido dinamicamente via `data.aws_vpc.existing.cidr_block`.

## Goals / Non-Goals

**Goals:**
- Adicionar regra inbound ao SG do RDS permitindo TCP 5432 via CIDR da VPC
- Adicionar regra inbound ao SG do RDS permitindo TCP 5432 via SG default da VPC (resolvido por data source)
- Atualizar a spec `security-group-chain` para refletir os novos requisitos

**Non-Goals:**
- Expor o RDS publicamente
- Alterar qualquer outra regra existente (ECS → RDS permanece intacta)
- Modificar subnets, parâmetros ou configurações do banco

## Decisions

### Decisão 1: Usar data source para o SG default em vez de hardcodar o ID

**Escolhido:** `data "aws_security_group" "default"` com `name = "default"` e `vpc_id = var.vpc_id`

**Alternativa descartada:** Hardcodar `sg-09fe29350b7ec6da4` diretamente no recurso

**Rationale:** O ID do SG default é específico de conta e VPC. Um data source torna o código portável, evita drift se o SG for recriado, e é mais legível — fica explícito que a fonte é o SG default da VPC.

### Decisão 2: Usar `data.aws_vpc.existing.cidr_block` em vez de hardcodar `172.31.0.0/16`

**Escolhido:** Referência dinâmica ao data source existente

**Rationale:** O data source `data.aws_vpc.existing` já é declarado no módulo. Reutilizá-lo garante consistência — se o CIDR da VPC mudar, a regra acompanha automaticamente.

### Decisão 3: Manter as duas regras existentes (ECS → RDS) inalteradas

As novas regras são **adicionadas**, não substituídas. A cadeia ECS → RDS continua funcionando normalmente.

## Risks / Trade-offs

- **Ampliação da superfície de acesso ao RDS** → A regra por CIDR da VPC (`172.31.0.0/16`) permite que qualquer recurso na VPC acesse o banco na porta 5432 — não só o CloudShell. Mitigation: aceitável em `dev`; para `prod` considerar restringir por SG específico em vez de CIDR.
- **SG default como fonte** → O SG default pode ser associado a outros recursos além do CloudShell. Em ambientes com múltiplos usuários, isso pode ampliar acesso indesejado. Mitigation: documentar o uso e rever em caso de escalonamento do ambiente.
- **Sem rollback automático** → Regras de SG são alteradas in-place pelo Terraform. Para reverter, basta remover os blocos `ingress` e reaplicar.
