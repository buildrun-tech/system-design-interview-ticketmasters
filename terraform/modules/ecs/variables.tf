# ECS Module Variables

variable "name_prefix" {
  description = "Prefix for resource names"
  type        = string
}

variable "common_tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
  default     = {}
}

variable "environment" {
  description = "Environment name (dev, prod)"
  type        = string
}

variable "aws_region" {
  description = "AWS region"
  type        = string
}

# Networking
variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "public_subnet_ids" {
  description = "List of public subnet IDs for ALB"
  type        = list(string)
}

variable "private_subnet_ids" {
  description = "List of private subnet IDs for ECS tasks"
  type        = list(string)
}

variable "alb_security_group_id" {
  description = "Security group ID for ALB"
  type        = string
}

variable "ecs_security_group_id" {
  description = "Security group ID for ECS tasks"
  type        = string
}

# ECS Configuration
variable "cluster_name" {
  description = "Name of the ECS cluster"
  type        = string
}

variable "service_name" {
  description = "Name of the ECS service"
  type        = string
}

variable "task_cpu" {
  description = "CPU units for ECS task"
  type        = number
}

variable "task_memory" {
  description = "Memory (MB) for ECS task"
  type        = number
}

variable "desired_count" {
  description = "Desired number of ECS tasks"
  type        = number
}

variable "container_port" {
  description = "Port on which the application container listens"
  type        = number
}

# ECR
variable "ecr_repository_url" {
  description = "URL of the ECR repository"
  type        = string
}

# Database
variable "db_endpoint" {
  description = "RDS endpoint"
  type        = string
}

variable "db_name" {
  description = "Database name"
  type        = string
}

variable "db_username" {
  description = "Database username"
  type        = string
}

variable "db_password_secret_arn" {
  description = "ARN of the secret containing database password"
  type        = string
}

# Load Balancer Configuration
variable "alb_name" {
  description = "Name of the Application Load Balancer"
  type        = string
}

variable "health_check_path" {
  description = "Health check path for ALB target group"
  type        = string
}

variable "health_check_interval" {
  description = "Health check interval in seconds"
  type        = number
}

variable "health_check_timeout" {
  description = "Health check timeout in seconds"
  type        = number
}

variable "health_check_healthy_threshold" {
  description = "Number of consecutive successful health checks"
  type        = number
}

variable "health_check_unhealthy_threshold" {
  description = "Number of consecutive failed health checks"
  type        = number
}

# Monitoring and Logging
variable "enable_container_insights" {
  description = "Enable CloudWatch Container Insights for ECS cluster"
  type        = bool
}

variable "log_retention_days" {
  description = "CloudWatch log retention period in days"
  type        = number
}

# Fargate Configuration
variable "use_fargate_spot" {
  description = "Use Fargate Spot instances (true for dev, false for prod)"
  type        = bool
  default     = false
}