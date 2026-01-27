# ECR Module - Container Registry

# ECR Repository for container images
resource "aws_ecr_repository" "app_repository" {
  name                 = "${var.name_prefix}-${var.repository_name}"
  image_tag_mutability = var.image_tag_mutability

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-${var.repository_name}"
  })
}

# ECR Repository Policy for cross-environment image promotion
resource "aws_ecr_repository_policy" "app_repository_policy" {
  repository = aws_ecr_repository.app_repository.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCrossEnvironmentImagePromotion"
        Effect = "Allow"
        Principal = {
          AWS = [
            # Allow DEV environment to push images
            "arn:aws:iam::${var.aws_account_id}:role/*-dev-*",
            # Allow PROD environment to pull and retag images
            "arn:aws:iam::${var.aws_account_id}:role/*-prod-*"
          ]
        }
        Action = [
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:BatchCheckLayerAvailability",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:DescribeRepositories",
          "ecr:GetRepositoryPolicy",
          "ecr:ListImages",
          "ecr:DescribeImages",
          "ecr:BatchDeleteImage"
        ]
      },
      {
        Sid    = "AllowECSTasksToAccessImages"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${var.aws_account_id}:role/*-ecs-task-execution-role"
        }
        Action = [
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:BatchCheckLayerAvailability"
        ]
      }
    ]
  })
}

# ECR Lifecycle Policy to manage image retention
resource "aws_ecr_lifecycle_policy" "app_repository_lifecycle" {
  repository = aws_ecr_repository.app_repository.name

  policy = var.lifecycle_policy
}