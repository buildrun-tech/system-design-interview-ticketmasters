# Development Environment Configuration

# Core Configuration
environment = "dev"
aws_region  = "us-east-2"

# Networking Configuration (using existing VPC)
# Replace these with your actual VPC and subnet IDs
vpc_id = "vpc-088d5d306dd51edda"  # Replace with your VPC ID
public_subnet_ids = [
  "subnet-0b86a3228eb03fee8",     # Replace with your public subnet IDs
  "subnet-0bbaa0ae622a7bed5",
  "subnet-05da6cbe10402cec9"
]

private_subnet_ids = [
  "subnet-0b86a3228eb03fee8",     # Replace with your public subnet IDs
  "subnet-0bbaa0ae622a7bed5",
  "subnet-05da6cbe10402cec9"
]

# ECR Configuration
ecr_repository_url = "311141562939.dkr.ecr.us-east-2.amazonaws.com/ticketmaster-dev"

# ECS Configuration
ecs_cluster_name  = "ticketmaster-dev-cluster"
ecs_service_name  = "ticketmaster-service"
ecs_task_cpu      = 512
ecs_task_memory   = 1024
ecs_desired_count = 1
container_port    = 8080

# Fargate Configuration (use Spot for cost savings in dev)
use_fargate_spot = true

# RDS Configuration (smaller instance for dev)
db_instance_class           = "db.t3.micro"
db_name                     = "ticketmasterdb"
db_username                 = "postgres"
db_port                     = 5432
db_allocated_storage        = 20
db_max_allocated_storage    = 50
db_backup_retention_period  = 3
db_backup_window           = "03:00-04:00"
db_maintenance_window      = "sun:04:00-sun:05:00"

# Load Balancer Configuration
alb_name                         = "ticketmaster-dev-alb"
health_check_path                = "/q/health"
health_check_interval            = 30
health_check_timeout             = 5
health_check_healthy_threshold   = 2
health_check_unhealthy_threshold = 3

# Security Configuration (more permissive for dev)
allowed_cidr_blocks = ["0.0.0.0/0"]

# Monitoring and Logging
enable_container_insights = true
log_retention_days       = 3

# Additional Tags
additional_tags = {
  Owner       = "DevTeam"
  CostCenter  = "Development"
  Purpose     = "Development Environment"
}