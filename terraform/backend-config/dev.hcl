# S3 Backend Configuration for DEV Environment

bucket         = "terraform-311141562939-statefile"
key            = "dev/terraform.tfstate"
region         = "us-east-2"
encrypt        = true
use_lockfile = true

# Optional: Enable versioning and server-side encryption
# These settings should be configured on the S3 bucket itself