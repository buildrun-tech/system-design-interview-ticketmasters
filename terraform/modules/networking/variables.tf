# Networking Module Variables

variable "name_prefix" {
  description = "Prefix for resource names"
  type        = string
}

variable "common_tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
  default     = {}
}

variable "vpc_id" {
  description = "ID of the existing VPC"
  type        = string
}

variable "public_subnet_ids" {
  description = "List of existing public subnet IDs"
  type        = list(string)
}

variable "private_subnet_ids" {
  description = "List of existing private subnet IDs"
  type        = list(string)
}

variable "allowed_cidr_blocks" {
  description = "CIDR blocks allowed to access the load balancer"
  type        = list(string)
}

variable "container_port" {
  description = "Port on which the application container listens"
  type        = number
}

variable "db_port" {
  description = "Database port"
  type        = number
}