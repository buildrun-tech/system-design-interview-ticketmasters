# Terraform Outputs for TicketMaster CI/CD Infrastructure

# ECS Outputs
output "ecs_cluster_id" {
  description = "ID of the ECS cluster"
  value       = module.ecs.cluster_id
}

output "ecs_cluster_arn" {
  description = "ARN of the ECS cluster"
  value       = module.ecs.cluster_arn
}

output "ecs_service_id" {
  description = "ID of the ECS service"
  value       = module.ecs.service_id
}

output "ecs_service_arn" {
  description = "ARN of the ECS service"
  value       = module.ecs.service_arn
}

output "ecs_task_definition_arn" {
  description = "ARN of the ECS task definition"
  value       = module.ecs.task_definition_arn
}

output "ecs_task_execution_role_arn" {
  description = "ARN of the ECS task execution role"
  value       = module.ecs.task_execution_role_arn
}

output "ecs_task_role_arn" {
  description = "ARN of the ECS task role"
  value       = module.ecs.task_role_arn
}

# RDS Outputs
output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = module.rds.endpoint
}

output "rds_port" {
  description = "RDS instance port"
  value       = module.rds.port
}

output "rds_database_name" {
  description = "Name of the database"
  value       = module.rds.database_name
}

output "rds_username" {
  description = "RDS master username"
  value       = module.rds.username
  sensitive   = true
}

output "rds_instance_id" {
  description = "RDS instance ID"
  value       = module.rds.instance_id
}

output "rds_instance_arn" {
  description = "RDS instance ARN"
  value       = module.rds.instance_arn
}

# Networking Outputs
output "vpc_id" {
  description = "ID of the VPC"
  value       = module.networking.vpc_id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC"
  value       = module.networking.vpc_cidr_block
}

# Load Balancer Outputs
output "nlb_arn" {
  description = "ARN of the Network Load Balancer"
  value       = module.ecs.nlb_arn
}

output "nlb_dns_name" {
  description = "DNS name of the Network Load Balancer"
  value       = module.ecs.nlb_dns_name
}

output "nlb_zone_id" {
  description = "Zone ID of the Network Load Balancer"
  value       = module.ecs.nlb_zone_id
}

# VPC Link Outputs
output "vpc_link_id" {
  description = "ID of the VPC Link for API Gateway"
  value       = module.ecs.vpc_link_id
}

output "vpc_link_arn" {
  description = "ARN of the VPC Link for API Gateway"
  value       = module.ecs.vpc_link_arn
}

# Security Group Outputs
output "nlb_security_group_id" {
  description = "ID of the NLB security group"
  value       = module.networking.nlb_security_group_id
}

output "ecs_security_group_id" {
  description = "ID of the ECS security group"
  value       = module.networking.ecs_security_group_id
}

output "rds_security_group_id" {
  description = "ID of the RDS security group"
  value       = module.networking.rds_security_group_id
}

# CloudWatch Outputs
output "cloudwatch_log_group_name" {
  description = "Name of the CloudWatch log group"
  value       = module.ecs.cloudwatch_log_group_name
}

output "cloudwatch_log_group_arn" {
  description = "ARN of the CloudWatch log group"
  value       = module.ecs.cloudwatch_log_group_arn
}

# Environment Information
output "environment" {
  description = "Environment name"
  value       = var.environment
}

output "aws_region" {
  description = "AWS region"
  value       = var.aws_region
}

output "aws_account_id" {
  description = "AWS account ID"
  value       = data.aws_caller_identity.current.account_id
}