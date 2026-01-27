# RDS Module Variables

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

variable "instance_class" {
  description = "RDS instance class"
  type        = string
}

variable "database_name" {
  description = "Name of the database"
  type        = string
}

variable "username" {
  description = "Database master username"
  type        = string
}

variable "port" {
  description = "Database port"
  type        = number
}

variable "allocated_storage" {
  description = "Allocated storage for RDS instance (GB)"
  type        = number
}

variable "max_allocated_storage" {
  description = "Maximum allocated storage for RDS instance (GB)"
  type        = number
}

variable "backup_retention_period" {
  description = "Backup retention period in days"
  type        = number
}

variable "backup_window" {
  description = "Preferred backup window"
  type        = string
}

variable "maintenance_window" {
  description = "Preferred maintenance window"
  type        = string
}

variable "private_subnet_ids" {
  description = "List of private subnet IDs for DB subnet group"
  type        = list(string)
}

variable "security_group_id" {
  description = "Security group ID for RDS instance"
  type        = string
}