# Networking Module - Security Groups for existing VPC infrastructure

# Data sources for existing VPC infrastructure
data "aws_vpc" "existing" {
  id = var.vpc_id
}

# Security Group for Network Load Balancer
resource "aws_security_group" "nlb" {
  name_prefix = "${var.name_prefix}-nlb-"
  vpc_id      = var.vpc_id

  ingress {
    description = "Traffic from VPC"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.existing.cidr_block]
  }

  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-nlb-sg"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# Security Group for ECS Tasks
resource "aws_security_group" "ecs_tasks" {
  name_prefix = "${var.name_prefix}-ecs-tasks-"
  vpc_id      = var.vpc_id

  ingress {
    description     = "HTTP from NLB"
    from_port       = var.container_port
    to_port         = var.container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.nlb.id]
  }

  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-ecs-tasks-sg"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# Security Group for RDS
resource "aws_security_group" "rds" {
  name_prefix = "${var.name_prefix}-rds-"
  vpc_id      = var.vpc_id

  ingress {
    description     = "PostgreSQL from ECS"
    from_port       = var.db_port
    to_port         = var.db_port
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-rds-sg"
  })

  lifecycle {
    create_before_destroy = true
  }
}