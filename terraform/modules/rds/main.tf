# RDS Module - PostgreSQL Database

# Generate random password for RDS
resource "random_password" "db_password" {
  length  = 16
  special = true
}

# Store database password in AWS Secrets Manager
resource "aws_secretsmanager_secret" "db_password" {
  name                    = "${var.name_prefix}-db-password"
  description             = "Database password for ${var.name_prefix}"
  recovery_window_in_days = 7

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-db-password"
  })
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = random_password.db_password.result
}

# DB Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "${var.name_prefix}-db-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-db-subnet-group"
  })
}

# DB Parameter Group
resource "aws_db_parameter_group" "postgres" {
  family = "postgres18"
  name   = "${var.name_prefix}-postgres-params"

  parameter {
    name  = "log_statement"
    value = "all"
  }

  parameter {
    name  = "log_min_duration_statement"
    value = "1000"
  }

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-postgres-params"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# RDS PostgreSQL Instance
resource "aws_db_instance" "postgres" {
  identifier = "${var.name_prefix}-postgres"

  # Engine configuration
  engine         = "postgres"
  engine_version = "18.1"
  instance_class = var.instance_class

  # Database configuration
  db_name  = var.database_name
  username = var.username
  password = random_password.db_password.result
  port     = var.port

  # Storage configuration
  allocated_storage     = var.allocated_storage
  max_allocated_storage = var.max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true

  # Network configuration
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.security_group_id]
  publicly_accessible    = false

  # Parameter and option groups
  parameter_group_name = aws_db_parameter_group.postgres.name

  # Backup configuration
  backup_retention_period = var.backup_retention_period
  backup_window          = var.backup_window
  maintenance_window     = var.maintenance_window

  # Monitoring disabled as requested
  monitoring_interval = 0

  performance_insights_enabled = true
  performance_insights_retention_period = 7

  # Deletion protection and final snapshot
  deletion_protection       = var.environment == "prod" ? true : false
  skip_final_snapshot      = var.environment == "dev" ? true : false
  final_snapshot_identifier = var.environment == "prod" ? "${var.name_prefix}-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}" : null

  # Enable automated minor version upgrades
  auto_minor_version_upgrade = true

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-postgres"
  })

  lifecycle {
    ignore_changes = [
      password,
      final_snapshot_identifier
    ]
  }
}