# S3 Backend Configuration for PROD Environment

bucket         = "ticketmaster-terraform-state-prod"
key            = "prod/terraform.tfstate"
region         = "us-east-1"
encrypt        = true
dynamodb_table = "ticketmaster-terraform-locks-prod"

# Optional: Enable versioning and server-side encryption
# These settings should be configured on the S3 bucket itself