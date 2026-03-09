# ECS Module Outputs

output "cluster_id" {
  description = "ID of the ECS cluster"
  value       = aws_ecs_cluster.main.id
}

output "cluster_arn" {
  description = "ARN of the ECS cluster"
  value       = aws_ecs_cluster.main.arn
}

output "service_id" {
  description = "ID of the ECS service"
  value       = aws_ecs_service.app.id
}

output "service_arn" {
  description = "ARN of the ECS service"
  value       = aws_ecs_service.app.id
}

output "task_definition_arn" {
  description = "ARN of the ECS task definition"
  value       = aws_ecs_task_definition.app.arn
}

output "task_execution_role_arn" {
  description = "ARN of the ECS task execution role"
  value       = aws_iam_role.ecs_task_execution_role.arn
}

output "task_role_arn" {
  description = "ARN of the ECS task role"
  value       = aws_iam_role.ecs_task_role.arn
}

output "nlb_arn" {
  description = "ARN of the Network Load Balancer"
  value       = aws_lb.main.arn
}

output "nlb_dns_name" {
  description = "DNS name of the Network Load Balancer"
  value       = aws_lb.main.dns_name
}

output "nlb_zone_id" {
  description = "Zone ID of the Network Load Balancer"
  value       = aws_lb.main.zone_id
}

output "vpc_link_id" {
  description = "ID of the VPC Link for API Gateway"
  value       = aws_api_gateway_vpc_link.main.id
}

output "vpc_link_arn" {
  description = "ARN of the VPC Link for API Gateway"
  value       = aws_api_gateway_vpc_link.main.arn
}

output "cloudwatch_log_group_name" {
  description = "Name of the CloudWatch log group"
  value       = aws_cloudwatch_log_group.app.name
}

output "cloudwatch_log_group_arn" {
  description = "ARN of the CloudWatch log group"
  value       = aws_cloudwatch_log_group.app.arn
}