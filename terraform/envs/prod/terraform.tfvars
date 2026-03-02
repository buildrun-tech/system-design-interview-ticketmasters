# Production Environment Configuration

# Core Configuration
environment = "prod"
aws_region  = "us-east-1"

# Networking Configuration (using existing VPC)
# Replace these with your actual VPC and subnet IDs
vpc_id = "vpc-xxxxxxxxx"  # Replace with your VPC ID
public_subnet_ids = [
  "subnet-xxxxxxxxx",     # Replace with your public subnet IDs
  "subnet-yyyyyyyyy"
]
private_subnet_ids = [
  "subnet-zzzzzzzzz",     # Replace with your private subnet IDs
  "subnet-aaaaaaaaa"
]

# ECR Configuration
ecr_repository_name      = "ticketmaster-app"
ecr_image_tag_mutability = "IMMUTABLE"

# ECS Configuration (higher capacity for prod)
ecs_cluster_name  = "ticketmaster-prod-cluster"
ecs_service_name  = "ticketmaster-service"
ecs_task_cpu      = 1024
ecs_task_memory   = 2048
ecs_desired_count = 2
container_port    = 8080

# Fargate Configuration (use On-Demand for reliability in prod)
use_fargate_spot = false

# RDS Configuration (larger instance for prod)
db_instance_class           = "db.t3.small"
db_name                     = "ticketmasterdb"
db_username                 = "postgres"
db_port                     = 5432
db_allocated_storage        = 50
db_max_allocated_storage    = 200
db_backup_retention_period  = 14
db_backup_window           = "03:00-04:00"
db_maintenance_window      = "sun:04:00-sun:05:00"

# Load Balancer Configuration
alb_name                         = "ticketmaster-prod-alb"
health_check_path                = "/q/health"
health_check_interval            = 30
health_check_timeout             = 5
health_check_healthy_threshold   = 3
health_check_unhealthy_threshold = 2

# Security Configuration (more restrictive for prod)
# Note: In real production, this should be restricted to specific CIDR blocks
allowed_cidr_blocks = ["0.0.0.0/0"]

# Monitoring and Logging
enable_container_insights = true
log_retention_days       = 30

# Additional Tags
additional_tags = {
  Owner       = "ProductionTeam"
  CostCenter  = "Production"
  Purpose     = "Production Environment"
  Compliance  = "Required"
}