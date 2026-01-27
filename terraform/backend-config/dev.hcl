# S3 Backend Configuration for DEV Environment

bucket         = "ticketmaster-terraform-state-dev"
key            = "dev/terraform.tfstate"
region         = "us-east-1"
encrypt        = true
dynamodb_table = "ticketmaster-terraform-locks-dev"

# Optional: Enable versioning and server-side encryption
# These settings should be configured on the S3 bucket itself