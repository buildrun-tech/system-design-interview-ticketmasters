# Main Terraform configuration for TicketMaster CI/CD Infrastructure

terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.1"
    }
  }

  backend "s3" {
    # Backend configuration will be provided via backend config file
    # or command line arguments during terraform init
  }
}

# AWS Provider configuration with OIDC assume role authentication
provider "aws" {
  region = var.aws_region
  
  # OIDC authentication will be handled by GitHub Actions
  # The provider will use the assumed role credentials from the environment
}

# Data source to get current AWS account information
data "aws_caller_identity" "current" {}

# Data source to get available AZs
data "aws_availability_zones" "available" {
  state = "available"
}

# Local values for common tags and naming
locals {
  common_tags = merge({
    Project     = "TicketMaster"
    Environment = var.environment
    ManagedBy   = "Terraform"
    Repository  = "system-design-interview-ticketmasters"
  }, var.additional_tags)
  
  name_prefix = "${var.project_name}-${var.environment}"
}

# Networking Module
module "networking" {
  source = "./modules/networking"

  name_prefix         = local.name_prefix
  common_tags        = local.common_tags
  vpc_id             = var.vpc_id
  allowed_cidr_blocks = var.allowed_cidr_blocks
  container_port     = var.container_port
  db_port           = var.db_port
}


# RDS Module
module "rds" {
  source = "./modules/rds"

  name_prefix             = local.name_prefix
  common_tags            = local.common_tags
  environment            = var.environment
  instance_class         = var.db_instance_class
  database_name          = var.db_name
  username               = var.db_username
  port                   = var.db_port
  allocated_storage      = var.db_allocated_storage
  max_allocated_storage  = var.db_max_allocated_storage
  backup_retention_period = var.db_backup_retention_period
  backup_window          = var.db_backup_window
  maintenance_window     = var.db_maintenance_window
  private_subnet_ids     = var.private_subnet_ids
  security_group_id      = module.networking.rds_security_group_id
}

# SQS Module - check-booking-pending-state queue
module "sqs_check_booking_pending_state" {
  source = "./modules/sqs"

  queue_name  = "${var.environment}-check-booking-pending-state"
  name_prefix = local.name_prefix
  common_tags = local.common_tags
}

# ECS Module
module "ecs" {
  source = "./modules/ecs"

  name_prefix            = local.name_prefix
  common_tags            = local.common_tags
  environment            = var.environment
  aws_region             = var.aws_region
  vpc_id                 = var.vpc_id
  private_subnet_ids     = var.private_subnet_ids
  nlb_security_group_id  = module.networking.nlb_security_group_id
  ecs_security_group_id  = module.networking.ecs_security_group_id
  cluster_name           = var.ecs_cluster_name != null ? var.ecs_cluster_name : "${local.name_prefix}-cluster"
  service_name           = var.ecs_service_name
  task_cpu               = var.ecs_task_cpu
  task_memory            = var.ecs_task_memory
  desired_count          = var.ecs_desired_count
  container_port         = var.container_port
  ecr_repository_url     = var.ecr_repository_url
  image_tag              = var.image_tag
  db_endpoint            = module.rds.endpoint
  db_name                = var.db_name
  db_username            = var.db_username
  db_password_secret_arn      = module.rds.password_secret_arn
  sqs_check_booking_queue_url = module.sqs_check_booking_pending_state.queue_url
  enable_container_insights   = var.enable_container_insights
  log_retention_days     = var.log_retention_days
  use_fargate_spot       = var.use_fargate_spot
}

# API Gateway Module
module "api_gateway" {
  source = "./modules/api-gateway"

  name_prefix          = local.name_prefix
  common_tags          = local.common_tags
  vpc_id               = var.vpc_id
  private_subnet_ids   = var.private_subnet_ids
  apigw_vpc_link_sg_id = module.networking.apigw_vpc_link_sg_id
  nlb_listener_arn     = module.ecs.nlb_listener_arn
}