# Networking Module Outputs

output "vpc_id" {
  description = "ID of the VPC"
  value       = var.vpc_id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC"
  value       = data.aws_vpc.existing.cidr_block
}

output "apigw_vpc_link_sg_id" {
  description = "Security group ID for the API Gateway VPC Link"
  value       = aws_security_group.apigw_vpc_link.id
}

output "nlb_security_group_id" {
  description = "Security group ID for NLB"
  value       = aws_security_group.nlb.id
}

output "ecs_security_group_id" {
  description = "ID of the ECS security group"
  value       = aws_security_group.ecs_tasks.id
}

output "rds_security_group_id" {
  description = "ID of the RDS security group"
  value       = aws_security_group.rds.id
}