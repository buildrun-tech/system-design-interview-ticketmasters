# ECS Module - Container Orchestration

# Network Load Balancer
resource "aws_lb" "main" {
  name                             = "${var.name_prefix}-nlb"
  internal                         = true
  load_balancer_type               = "network"
  security_groups                  = [var.nlb_security_group_id]
  subnets                          = var.private_subnet_ids
  enable_cross_zone_load_balancing = true
  enable_deletion_protection       = false

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-nlb"
  })
}

# NLB Target Group
resource "aws_lb_target_group" "main" {
  name                 = "${var.name_prefix}-nlb-tg"
  port                 = var.container_port
  protocol             = "TCP"
  vpc_id               = var.vpc_id
  target_type          = "ip"
  deregistration_delay = 30

  health_check {
    enabled             = true
    healthy_threshold   = 3
    unhealthy_threshold = 3
    interval            = 30
    port                = "traffic-port"
    protocol            = "HTTP"
    path                = "/q/health/ready"
    matcher             = "200"
  }

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-nlb-tg"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# NLB Listener
resource "aws_lb_listener" "main" {
  load_balancer_arn = aws_lb.main.arn
  port              = 8080
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.main.arn
  }
}

# VPC Link for API Gateway
resource "aws_api_gateway_vpc_link" "main" {
  name        = "${var.name_prefix}-vpc-link"
  description = "VPC Link for API Gateway to access internal NLB"
  target_arns = [aws_lb.main.arn]

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-vpc-link"
  })
}

# CloudWatch Log Group for ECS
resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.name_prefix}"
  retention_in_days = var.log_retention_days

  tags = merge(var.common_tags, {
    Name = "/ecs/${var.name_prefix}"
  })
}

# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = var.cluster_name

  setting {
    name  = "containerInsights"
    value = var.enable_container_insights ? "enabled" : "disabled"
  }

  tags = merge(var.common_tags, {
    Name = var.cluster_name
  })
}

# ECS Cluster Capacity Providers
resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name = aws_ecs_cluster.main.name

  capacity_providers = var.use_fargate_spot ? ["FARGATE", "FARGATE_SPOT"] : ["FARGATE"]

  default_capacity_provider_strategy {
    base              = 0
    capacity_provider = var.use_fargate_spot ? "FARGATE_SPOT" : "FARGATE"
    weight            = 100
  }
}

# ECS Task Execution Role
resource "aws_iam_role" "ecs_task_execution_role" {
  name = "${var.name_prefix}-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-ecs-task-execution-role"
  })
}

# Attach AWS managed policy for ECS task execution
resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Additional policy for ECR access
resource "aws_iam_role_policy" "ecs_task_execution_ecr_policy" {
  name = "${var.name_prefix}-ecs-task-execution-ecr-policy"
  role = aws_iam_role.ecs_task_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage"
        ]
        Resource = "*"
      }
    ]
  })
}

# Grant ECS task execution role access to secrets
resource "aws_iam_role_policy" "ecs_secrets_policy" {
  name = "${var.name_prefix}-ecs-secrets-policy"
  role = aws_iam_role.ecs_task_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = [
          var.db_password_secret_arn
        ]
      }
    ]
  })
}

# ECS Task Role (for application permissions)
resource "aws_iam_role" "ecs_task_role" {
  name = "${var.name_prefix}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-ecs-task-role"
  })
}

# SQS Policy for ECS Task Role
resource "aws_iam_role_policy" "ecs_task_sqs_policy" {
  name = "${var.name_prefix}-ecs-task-sqs-policy"
  role = aws_iam_role.ecs_task_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes",
          "sqs:GetQueueUrl",
          "sqs:ListQueues",
          "sqs:ChangeMessageVisibility",
          "sqs:PurgeQueue"
        ]
        Resource = "*"
      }
    ]
  })
}

# ECS Task Definition
resource "aws_ecs_task_definition" "app" {
  family                   = "${var.name_prefix}-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name  = "ticketmaster-app"
      image = "${var.ecr_repository_url}:${var.image_tag != null ? var.image_tag : "latest-${var.environment}"}"
      
      portMappings = [
        {
          containerPort = var.container_port
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "QUARKUS_PROFILE"
          value = var.environment
        },
        {
          name  = "QUARKUS_DATASOURCE_JDBC_URL"
          value = "jdbc:postgresql://${var.db_endpoint}/${var.db_name}"
        },
        {
          name  = "QUARKUS_DATASOURCE_USERNAME"
          value = var.db_username
        },
        {
          name  = "SQS_CHECK_BOOKING_PENDING_STATE_QUEUE_NAME",
          value = "${var.environment}-check-booking-pending-state"
        }
      ]

      secrets = [
        {
          name      = "QUARKUS_DATASOURCE_PASSWORD"
          valueFrom = var.db_password_secret_arn
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.app.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      essential = true
    }
  ])

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-task"
  })
}

# ECS Service
resource "aws_ecs_service" "app" {
  name            = var.service_name
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.desired_count

  # Capacity provider strategy for Fargate Spot support
  dynamic "capacity_provider_strategy" {
    for_each = var.use_fargate_spot ? [1] : []
    content {
      capacity_provider = "FARGATE_SPOT"
      weight           = 100
      base             = 0
    }
  }

  # Use regular Fargate if Spot is not enabled
  launch_type = var.use_fargate_spot ? null : "FARGATE"

  network_configuration {
    security_groups  = [var.ecs_security_group_id]
    subnets          = var.private_subnet_ids
    assign_public_ip = var.assign_public_ip
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.main.arn
    container_name   = "ticketmaster-app"
    container_port   = var.container_port
  }

  depends_on = [aws_lb_listener.main]

  tags = merge(var.common_tags, {
    Name = var.service_name
  })
}
