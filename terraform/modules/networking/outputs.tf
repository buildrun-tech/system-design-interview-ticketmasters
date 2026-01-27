# Networking Module Outputs

output "vpc_id" {
  description = "ID of the VPC"
  value       = var.vpc_id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC"
  value       = data.aws_vpc.existing.cidr_block
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = var.public_subnet_ids
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = var.private_subnet_ids
}

output "alb_security_group_id" {
  description = "ID of the ALB security group"
  value       = aws_security_group.alb.id
}

output "ecs_security_group_id" {
  description = "ID of the ECS security group"
  value       = aws_security_group.ecs_tasks.id
}

output "rds_security_group_id" {
  description = "ID of the RDS security group"
  value       = aws_security_group.rds.id
}