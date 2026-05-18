## ADDED Requirements

### Requirement: Terraform não sobrescreve task count gerenciado pelo auto-scaler
Quando o auto-scaling está habilitado, o sistema SHALL preservar o número de tasks gerenciado pelo Application Auto Scaling entre deployments Terraform consecutivos.

#### Scenario: Apply subsequente não reseta desired_count
- **WHEN** o auto-scaler escalou o serviço para 4 tasks e um `terraform apply` é executado sem mudanças na task definition
- **THEN** o ECS service permanece com 4 tasks em execução após o apply

#### Scenario: Apply com nova task definition não afeta task count
- **WHEN** o Terraform registra uma nova revisão da task definition e atualiza o ECS service
- **THEN** o rolling deployment usa o número de tasks atual gerenciado pelo auto-scaler, não o valor estático da variável `desired_count`
