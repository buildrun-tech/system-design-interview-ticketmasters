# TicketMaster Terraform Infrastructure

This directory contains Terraform configurations for provisioning AWS infrastructure for the TicketMaster application across DEV and PROD environments.

## Directory Structure

```
terraform/
├── main.tf                    # Main Terraform configuration
├── variables.tf               # Variable definitions
├── outputs.tf                 # Output definitions
├── environments/              # Environment-specific variable files
│   ├── dev.tfvars            # DEV environment variables
│   └── prod.tfvars           # PROD environment variables
├── backend-config/            # S3 backend configuration files
│   ├── dev.hcl               # DEV backend config
│   └── prod.hcl              # PROD backend config
└── README.md                 # This file
```

## Prerequisites

1. **AWS Account Setup**: Ensure you have an AWS account with appropriate permissions
2. **OIDC Identity Provider**: Configure GitHub OIDC Identity Provider in AWS IAM
3. **IAM Roles**: Create environment-specific IAM roles with trust relationships to GitHub
4. **S3 Buckets**: Create S3 buckets for Terraform state storage (see Backend Setup below)
5. **DynamoDB Tables**: Create DynamoDB tables for state locking

## Backend Setup

Before using Terraform, you need to create the S3 buckets and DynamoDB tables for state management:

### S3 Buckets for State Storage

```bash
# Create DEV state bucket
aws s3 mb s3://ticketmaster-terraform-state-dev --region us-east-1
aws s3api put-bucket-versioning --bucket ticketmaster-terraform-state-dev --versioning-configuration Status=Enabled
aws s3api put-bucket-encryption --bucket ticketmaster-terraform-state-dev --server-side-encryption-configuration '{
  "Rules": [
    {
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }
  ]
}'

# Create PROD state bucket
aws s3 mb s3://ticketmaster-terraform-state-prod --region us-east-1
aws s3api put-bucket-versioning --bucket ticketmaster-terraform-state-prod --versioning-configuration Status=Enabled
aws s3api put-bucket-encryption --bucket ticketmaster-terraform-state-prod --server-side-encryption-configuration '{
  "Rules": [
    {
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }
  ]
}'
```

### DynamoDB Tables for State Locking

```bash
# Create DEV lock table
aws dynamodb create-table \
  --table-name ticketmaster-terraform-locks-dev \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
  --region us-east-1

# Create PROD lock table
aws dynamodb create-table \
  --table-name ticketmaster-terraform-locks-prod \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
  --region us-east-1
```

## Usage

### Initialize Terraform for DEV Environment

```bash
cd terraform
terraform init -backend-config=backend-config/dev.hcl
```

### Initialize Terraform for PROD Environment

```bash
cd terraform
terraform init -backend-config=backend-config/prod.hcl -reconfigure
```

### Plan and Apply Changes

#### DEV Environment
```bash
# Plan changes
terraform plan -var-file=environments/dev.tfvars

# Apply changes
terraform apply -var-file=environments/dev.tfvars
```

#### PROD Environment
```bash
# Plan changes
terraform plan -var-file=environments/prod.tfvars

# Apply changes
terraform apply -var-file=environments/prod.tfvars
```

### Destroy Infrastructure

#### DEV Environment
```bash
terraform destroy -var-file=environments/dev.tfvars
```

#### PROD Environment
```bash
terraform destroy -var-file=environments/prod.tfvars
```

## OIDC Authentication

This Terraform configuration is designed to work with GitHub Actions using OIDC authentication. The AWS provider will automatically use the credentials from the assumed IAM role.

### Required IAM Permissions

The IAM roles used by GitHub Actions need the following permissions:

- **ECR**: Full access to manage repositories and images
- **ECS**: Full access to manage clusters, services, and tasks
- **RDS**: Full access to manage database instances
- **VPC**: Full access to manage networking resources
- **IAM**: PassRole permission for ECS task execution
- **CloudWatch**: Full access for logging and monitoring
- **S3**: Access to Terraform state buckets
- **DynamoDB**: Access to Terraform lock tables

## Environment Differences

### DEV Environment
- Smaller instance sizes for cost optimization
- Shorter backup retention periods
- More permissive security settings for development
- Container insights enabled for debugging

### PROD Environment
- Larger instance sizes for performance
- Longer backup retention periods
- More restrictive security settings
- Enhanced monitoring and logging
- Multiple ECS tasks for high availability

## Security Considerations

1. **State Files**: Terraform state files contain sensitive information and are encrypted at rest in S3
2. **Database Passwords**: RDS passwords are managed by AWS and not stored in state files
3. **Network Security**: Security groups restrict access between components
4. **OIDC Authentication**: No long-term AWS credentials are stored in GitHub

## Troubleshooting

### Common Issues

1. **Backend Initialization Errors**: Ensure S3 buckets and DynamoDB tables exist
2. **Permission Errors**: Verify IAM roles have required permissions
3. **State Lock Errors**: Check DynamoDB table accessibility
4. **Resource Conflicts**: Ensure resource names are unique across environments

### Useful Commands

```bash
# Check Terraform state
terraform show

# List resources in state
terraform state list

# Import existing resources
terraform import aws_s3_bucket.example bucket-name

# Refresh state
terraform refresh -var-file=environments/dev.tfvars
```