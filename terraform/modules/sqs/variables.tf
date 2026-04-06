# SQS Module Variables

variable "queue_name" {
  description = "Name of the SQS queue"
  type        = string
}

variable "name_prefix" {
  description = "Prefix for resource names"
  type        = string
}

variable "common_tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
  default     = {}
}

variable "visibility_timeout_seconds" {
  description = "Visibility timeout for messages in seconds"
  type        = number
  default     = 30
}

variable "message_retention_seconds" {
  description = "Message retention period in seconds (default: 4 days)"
  type        = number
  default     = 345600
}

variable "max_receive_count" {
  description = "Number of times a message can be received before being moved to the DLQ"
  type        = number
  default     = 5
}
