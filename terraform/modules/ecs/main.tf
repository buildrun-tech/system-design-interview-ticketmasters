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

# CloudWatch Log Group for ECS
resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.name_prefix}"
  retention_in_days = var.log_retention_days

  tags = merge(var.common_tags, {
    Name = "/ecs/${var.name_prefix}"
  })
}

# CloudWatch Log Group for ADOT Collector sidecar (task 3.3)
resource "aws_cloudwatch_log_group" "otel_collector" {
  name              = "/ecs/${var.name_prefix}-otel-collector"
  retention_in_days = var.log_retention_days

  tags = merge(var.common_tags, {
    Name = "/ecs/${var.name_prefix}-otel-collector"
  })
}

# SSM Parameter — ADOT Collector configuration YAML (task 2.2)
resource "aws_ssm_parameter" "otel_collector_config" {
  name  = "/${var.name_prefix}/otel-collector-config"
  type  = "String"
  value = templatefile("${path.module}/otel-collector-config.yaml.tftpl", {
    aws_region  = var.aws_region
    name_prefix = var.name_prefix
  })

  tags = merge(var.common_tags, {
    Name = "/${var.name_prefix}/otel-collector-config"
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

# SSM policy for ECS Task Execution Role — reads ADOT Collector config at task startup (task 3.5)
resource "aws_iam_role_policy" "ecs_task_execution_ssm_policy" {
  name = "${var.name_prefix}-ecs-task-execution-ssm-policy"
  role = aws_iam_role.ecs_task_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ssm:GetParameter",
          "ssm:GetParameters"
        ]
        Resource = aws_ssm_parameter.otel_collector_config.arn
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
          var.db_password_secret_arn,
          aws_secretsmanager_secret.jwt_keys.arn
        ]
      }
    ]
  })
}

# Secret container for JWT signing/verification keys — value is populated manually
# outside Terraform (no aws_secretsmanager_secret_version here), so the PEM
# content never lands in the .tfstate.
resource "aws_secretsmanager_secret" "jwt_keys" {
  name                    = "${var.name_prefix}-jwt-keys"
  description             = "JWT public/private key pair (PEM) for ${var.name_prefix} — value populated manually"
  recovery_window_in_days = 0

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-jwt-keys"
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

# CloudWatch policy for ECS Task Role — allows ADOT Collector sidecar to publish EMF metrics (task 3.4)
resource "aws_iam_role_policy" "ecs_task_cloudwatch_policy" {
  name = "${var.name_prefix}-ecs-task-cloudwatch-policy"
  role = aws_iam_role.ecs_task_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "cloudwatch:PutMetricData",
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogStreams"
        ]
        Resource = "*"
      }
    ]
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
          name  = "SQS_CHECK_BOOKING_PENDING_STATE_QUEUE_URL"
          value = var.sqs_check_booking_queue_url
        },
        {
          name  = "OTEL_EXPORTER_OTLP_METRICS_ENDPOINT"
          value = "http://localhost:4318"
        }
      ]

      secrets = [
        {
          name      = "QUARKUS_DATASOURCE_PASSWORD"
          valueFrom = var.db_password_secret_arn
        },
        {
          name      = "MP_JWT_VERIFY_PUBLICKEY"
          valueFrom = "${aws_secretsmanager_secret.jwt_keys.arn}:publicKey::"
        },
        {
          name      = "SMALLRYE_JWT_SIGN_KEY"
          valueFrom = "${aws_secretsmanager_secret.jwt_keys.arn}:privateKey::"
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
    },
    # ADOT Collector sidecar — receives OTLP metrics from the app and exports to CloudWatch EMF (task 3.2)
    {
      name  = "aws-otel-collector"
      image = var.adot_collector_image
      cpu   = 256
      memoryReservation = 256

      secrets = [
        {
          name      = "AOT_CONFIG_CONTENT"
          valueFrom = aws_ssm_parameter.otel_collector_config.arn
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.otel_collector.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      # essential: false — collector failure must not take down the main app
      essential = false
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

  lifecycle {
    ignore_changes = [desired_count]
  }

  tags = merge(var.common_tags, {
    Name = var.service_name
  })
}

# Application Auto Scaling Target
resource "aws_appautoscaling_target" "ecs" {
  max_capacity       = var.autoscaling_max_capacity
  min_capacity       = var.autoscaling_min_capacity
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.app.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

# Step Scaling Policy — Scale Out
resource "aws_appautoscaling_policy" "ecs_step_scale_out" {
  name               = "${var.name_prefix}-cpu-step-scale-out"
  policy_type        = "StepScaling"
  resource_id        = aws_appautoscaling_target.ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs.service_namespace

  step_scaling_policy_configuration {
    adjustment_type          = "ChangeInCapacity"
    cooldown                 = var.autoscaling_scale_out_cooldown
    metric_aggregation_type  = "Average"

    step_adjustment {
      metric_interval_lower_bound = 0
      metric_interval_upper_bound = 10
      scaling_adjustment          = 1
    }

    step_adjustment {
      metric_interval_lower_bound = 10
      metric_interval_upper_bound = 25
      scaling_adjustment          = 2
    }

    step_adjustment {
      metric_interval_lower_bound = 25
      scaling_adjustment          = 4
    }
  }
}

# Step Scaling Policy — Scale In
resource "aws_appautoscaling_policy" "ecs_step_scale_in" {
  name               = "${var.name_prefix}-cpu-step-scale-in"
  policy_type        = "StepScaling"
  resource_id        = aws_appautoscaling_target.ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs.service_namespace

  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = var.autoscaling_scale_in_cooldown
    metric_aggregation_type = "Average"

    step_adjustment {
      metric_interval_upper_bound = 0
      scaling_adjustment          = -1
    }
  }
}

# CloudWatch Alarm — Scale Out (high-resolution: 30s, 1 evaluation period)
resource "aws_cloudwatch_metric_alarm" "ecs_cpu_scale_out" {
  alarm_name          = "${var.name_prefix}-ecs-cpu-scale-out"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = 30
  statistic           = "Average"
  threshold           = var.autoscaling_scale_out_cpu_threshold

  dimensions = {
    ClusterName = aws_ecs_cluster.main.name
    ServiceName = aws_ecs_service.app.name
  }

  alarm_actions = [aws_appautoscaling_policy.ecs_step_scale_out.arn]
}

# CloudWatch Alarm — Scale In (standard resolution: 60s, 3 evaluation periods)
resource "aws_cloudwatch_metric_alarm" "ecs_cpu_scale_in" {
  alarm_name          = "${var.name_prefix}-ecs-cpu-scale-in"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 3
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = 60
  statistic           = "Average"
  threshold           = var.autoscaling_scale_in_cpu_threshold

  dimensions = {
    ClusterName = aws_ecs_cluster.main.name
    ServiceName = aws_ecs_service.app.name
  }

  alarm_actions = [aws_appautoscaling_policy.ecs_step_scale_in.arn]
}

# ─────────────────────────────────────────────────────────────────────────────
# 4 Golden Signals — CloudWatch Dashboard and Alarms
# Metrics are published by the ADOT Collector sidecar via EMF to TicketMaster/App
# ─────────────────────────────────────────────────────────────────────────────

# CloudWatch Dashboard — 4 Golden Signals (task 4.1)
resource "aws_cloudwatch_dashboard" "golden_signals" {
  dashboard_name = "${var.name_prefix}-golden-signals"

  dashboard_body = jsonencode({
    widgets = [
      # Golden Signal #1 — Latência (avg / min / max)
      # Micrometer OTLP exports timers as StatisticSet (Sum/Count/Min/Max) — percentile
      # statistics (p50/p95/p99) require explicit histogram buckets and return 0 without them.
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          title  = "Latência HTTP (avg / min / max)"
          view   = "timeSeries"
          period = 60
          region = var.aws_region
          yAxis  = { left = { label = "Seconds", showUnits = false } }
          metrics = [
            ["TicketMaster/App", "http.server.requests", "service.name", "ticketmaster", { stat = "Average", label = "avg", color = "#2ca02c" }],
            ["TicketMaster/App", "http.server.requests", "service.name", "ticketmaster", { stat = "Minimum", label = "min", color = "#1f77b4" }],
            ["TicketMaster/App", "http.server.requests", "service.name", "ticketmaster", { stat = "Maximum", label = "max", color = "#d62728" }],
          ]
        }
      },
      # Golden Signal #2 — Tráfego (Requests/min)
      # SampleCount maps to the Count field of the EMF StatisticSet = number of requests per period.
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          title  = "Tráfego (Requests / min)"
          view   = "timeSeries"
          period = 60
          region = var.aws_region
          metrics = [
            ["TicketMaster/App", "http.server.requests", "service.name", "ticketmaster", { stat = "SampleCount", label = "Requests/min", color = "#1f77b4" }],
          ]
        }
      },
      # Golden Signal #3 — Erros (4xx / 5xx count)
      # FILL(m, 0) replaces gaps (no errors = no datapoint published) with zero,
      # so the chart shows spikes only when errors actually occurred.
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6
        properties = {
          title  = "Erros HTTP (4xx / 5xx)"
          view   = "timeSeries"
          period = 60
          region = var.aws_region
          metrics = [
            [{ id = "e4xx", expression = "FILL(m4xx,0)", label = "4xx (Client Error)", color = "#ff7f0e" }],
            [{ id = "e5xx", expression = "FILL(m5xx,0)", label = "5xx (Server Error)", color = "#d62728" }],
            ["TicketMaster/App", "http.server.requests", "service.name", "ticketmaster", "outcome", "CLIENT_ERROR", { id = "m4xx", stat = "SampleCount", visible = false }],
            ["TicketMaster/App", "http.server.requests", "service.name", "ticketmaster", "outcome", "SERVER_ERROR", { id = "m5xx", stat = "SampleCount", visible = false }],
          ]
        }
      },
      # Golden Signal #4 — Saturação (JVM Heap / DB Pool)
      {
        type   = "metric"
        x      = 12
        y      = 6
        width  = 12
        height = 6
        properties = {
          title  = "Saturação (JVM Heap / DB Pool)"
          view   = "timeSeries"
          period = 60
          region = var.aws_region
          yAxis = {
            left  = { label = "Bytes", showUnits = false }
            right = { label = "Connections", showUnits = false, min = 0 }
          }
          metrics = [
            ["TicketMaster/App", "jvm.memory.used", "service.name", "ticketmaster", "area", "heap", { stat = "Maximum", label = "JVM Heap Used (bytes)", color = "#9467bd" }],
            ["TicketMaster/App", "agroal.connections.active", "service.name", "ticketmaster", { stat = "Average", label = "DB Active Connections", color = "#8c564b", yAxis = "right" }],
          ]
        }
      },
      # ECS Task — CPU Utilization (% of task CPU limit)
      {
        type   = "metric"
        x      = 0
        y      = 12
        width  = 12
        height = 6
        properties = {
          title  = "CPU Utilization (% do limit da task)"
          view   = "timeSeries"
          period = 60
          region = var.aws_region
          yAxis  = { left = { min = 0, max = 100, label = "%", showUnits = false } }
          metrics = [
            ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.app.name, { stat = "Average", label = "CPU avg (%)", color = "#e377c2" }],
            ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.app.name, { stat = "Maximum", label = "CPU max (%)", color = "#d62728" }],
          ]
        }
      },
      # ECS Task — Memory Utilization (% of task memory limit)
      {
        type   = "metric"
        x      = 12
        y      = 12
        width  = 12
        height = 6
        properties = {
          title  = "Memory Utilization (% do limit da task)"
          view   = "timeSeries"
          period = 60
          region = var.aws_region
          yAxis  = { left = { min = 0, max = 100, label = "%", showUnits = false } }
          metrics = [
            ["AWS/ECS", "MemoryUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.app.name, { stat = "Average", label = "Memory avg (%)", color = "#17becf" }],
            ["AWS/ECS", "MemoryUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.app.name, { stat = "Maximum", label = "Memory max (%)", color = "#9467bd" }],
          ]
        }
      }
    ]
  })
}

# Alarm — Latência máxima alta (task 4.2)
# extended_statistic "p99" was removed: Micrometer OTLP exports timers as a
# StatisticSet (Sum/Count/Min/Max), which has no histogram buckets, so
# CloudWatch extended (percentile) statistics always evaluate to 0/no data
# and the alarm never fired. Maximum is the closest equivalent CloudWatch
# can compute from a StatisticSet.
resource "aws_cloudwatch_metric_alarm" "http_latency_p99" {
  alarm_name          = "${var.name_prefix}-http-latency-max"
  alarm_description   = "HTTP max latency exceeded ${var.latency_p99_threshold_seconds}s"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "http.server.requests"
  namespace           = "TicketMaster/App"
  period              = 60
  statistic           = "Maximum"
  threshold           = var.latency_p99_threshold_seconds
  treat_missing_data  = "notBreaching"

  dimensions = {
    "service.name" = "ticketmaster"
  }

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-http-latency-p99"
  })
}

# Alarm — Taxa de erros 5xx (task 4.3)
resource "aws_cloudwatch_metric_alarm" "http_error_rate" {
  alarm_name          = "${var.name_prefix}-http-error-rate-5xx"
  alarm_description   = "HTTP 5xx error rate exceeded ${var.error_rate_threshold_percent}%"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  threshold           = var.error_rate_threshold_percent
  treat_missing_data  = "notBreaching"

  metric_query {
    id          = "errors"
    return_data = false
    metric {
      metric_name = "http.server.requests"
      namespace   = "TicketMaster/App"
      period      = 60
      stat        = "SampleCount"
      dimensions = {
        "service.name" = "ticketmaster"
        "outcome"      = "SERVER_ERROR"
      }
    }
  }

  metric_query {
    id          = "total"
    return_data = false
    metric {
      metric_name = "http.server.requests"
      namespace   = "TicketMaster/App"
      period      = 60
      stat        = "SampleCount"
      dimensions = {
        "service.name" = "ticketmaster"
      }
    }
  }

  metric_query {
    id          = "error_rate"
    expression  = "IF(total > 0, 100 * errors / total, 0)"
    label       = "5xx Error Rate (%)"
    return_data = true
  }

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-http-error-rate-5xx"
  })
}
