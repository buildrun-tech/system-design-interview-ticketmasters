# ECR Module Variables

variable "name_prefix" {
  description = "Prefix for resource names"
  type        = string
}

variable "common_tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
  default     = {}
}

variable "repository_name" {
  description = "Name of the ECR repository"
  type        = string
}

variable "image_tag_mutability" {
  description = "Image tag mutability setting for ECR repository"
  type        = string
  default     = "MUTABLE"
  validation {
    condition     = contains(["MUTABLE", "IMMUTABLE"], var.image_tag_mutability)
    error_message = "ECR image tag mutability must be either 'MUTABLE' or 'IMMUTABLE'."
  }
}

variable "lifecycle_policy" {
  description = "Lifecycle policy for ECR repository"
  type        = string
}

variable "aws_account_id" {
  description = "AWS account ID"
  type        = string
}