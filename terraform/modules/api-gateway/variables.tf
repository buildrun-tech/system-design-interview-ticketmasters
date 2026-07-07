# API Gateway Module Variables

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
  description = "ID of the VPC"
  type        = string
}

variable "private_subnet_ids" {
  description = "List of private subnet IDs for VPC Link ENIs"
  type        = list(string)
}

variable "apigw_vpc_link_sg_id" {
  description = "Security group ID to attach to the VPC Link (created in the networking module)"
  type        = string
}

variable "nlb_listener_arn" {
  description = "ARN of the NLB listener used as the HTTP_PROXY integration target"
  type        = string
}
