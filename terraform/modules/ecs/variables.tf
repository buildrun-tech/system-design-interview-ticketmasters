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

variable "private_subnet_ids" {
  description = "List of subnet IDs for NLB and ECS tasks"
  type        = list(string)
}

variable "assign_public_ip" {
  description = "Whether ECS task ENIs should receive public IP addresses"
  type        = bool
  default     = true
}

variable "nlb_security_group_id" {
  description = "Security group ID for NLB"
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

# Load Balancer Configuration - removed ALB-specific variables for NLB

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

# SQS
variable "sqs_check_booking_queue_url" {
  description = "URL of the SQS queue for check-booking-pending-state"
  type        = string
}

# Deployment Image
variable "image_tag" {
  description = "Image tag for the ECS task definition. When null, falls back to latest-<environment>."
  type        = string
  default     = null
}

# Auto Scaling
variable "autoscaling_min_capacity" {
  description = "Minimum number of ECS tasks for auto-scaling"
  type        = number
  default     = 1
}

variable "autoscaling_max_capacity" {
  description = "Maximum number of ECS tasks for auto-scaling"
  type        = number
  default     = 10
}

variable "autoscaling_cpu_target" {
  description = "Target CPU utilization percentage for Target Tracking scaling policy"
  type        = number
  default     = 70
}

variable "autoscaling_scale_in_cooldown" {
  description = "Cooldown period in seconds before another scale-in can happen"
  type        = number
  default     = 300
}

variable "autoscaling_scale_out_cooldown" {
  description = "Cooldown period in seconds before another scale-out can happen"
  type        = number
  default     = 60
}
