# SQS Module - Standard Queue with Dead-Letter Queue

# Dead-Letter Queue
resource "aws_sqs_queue" "dlq" {
  name                       = "${var.queue_name}-dlq"
  message_retention_seconds  = var.message_retention_seconds
  visibility_timeout_seconds = var.visibility_timeout_seconds

  tags = merge(var.common_tags, {
    Name = "${var.queue_name}-dlq"
  })
}

# Main Queue
resource "aws_sqs_queue" "main" {
  name                       = var.queue_name
  visibility_timeout_seconds = var.visibility_timeout_seconds
  message_retention_seconds  = var.message_retention_seconds

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = var.max_receive_count
  })

  tags = merge(var.common_tags, {
    Name = var.queue_name
  })
}
