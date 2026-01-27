# Terraform Modules

This directory contains reusable Terraform modules for the TicketMaster CI/CD infrastructure.

## Module Structure

### Networking Module (`networking/`)
Manages security groups for existing VPC infrastructure.

**Resources:**
- Security groups for ALB, ECS, and RDS
- Data sources for existing VPC and subnets

**Inputs:**
- Existing VPC ID and subnet IDs
- Security configuration parameters

**Outputs:**
- Security group IDs for use by other modules

### ECR Module (`ecr/`)
Manages Amazon Elastic Container Registry for Docker images.

**Resources:**
- ECR repository with encryption and scanning
- Repository policies for cross-environment access
- Lifecycle policies for image retention

**Outputs:**
- Repository URL, ARN, and registry ID
- Repository name for reference

### ECS Module (`ecs/`)
Manages Amazon Elastic Container Service for application deployment.

**Resources:**
- Application Load Balancer and target groups
- ECS cluster with Container Insights
- ECS task definition and service with Fargate Spot support
- IAM roles for task execution and application (includes SQS permissions)
- CloudWatch log groups

**Features:**
- Fargate Spot support (configurable per environment)
- SQS permissions for application messaging
- Auto-scaling and load balancing

**Outputs:**
- ECS cluster and service details
- Load balancer information
- IAM role ARNs
- Application URL

### RDS Module (`rds/`)
Manages Amazon RDS PostgreSQL database.

**Resources:**
- RDS PostgreSQL 18.1 instance with encryption
- DB subnet group and parameter group
- Secrets Manager for password storage
- Performance Insights enabled

**Features:**
- PostgreSQL 18.1 with optimized configuration
- Enhanced monitoring disabled for cost optimization
- Environment-specific deletion protection

**Outputs:**
- Database endpoint and connection details
- Secret ARN for password access
- Instance ID and ARN

## Usage

Each module is designed to work with existing AWS infrastructure rather than creating new VPC resources. The main Terraform configuration (`../main.tf`) orchestrates these modules with proper dependencies and data flow.

### Module Dependencies

```
existing VPC → networking (security groups)
networking → ecr (independent)
networking → rds
networking + ecr + rds → ecs
```

### Environment-Specific Configuration

Modules receive environment-specific configuration through variables, allowing the same modules to be used for both DEV and PROD environments with different settings:

- **DEV**: Uses Fargate Spot for cost savings, smaller RDS instances, shorter log retention
- **PROD**: Uses Fargate On-Demand for reliability, larger RDS instances, longer log retention

## Prerequisites

Before using these modules, you need:

1. **Existing VPC Infrastructure**:
   - VPC with public and private subnets
   - Internet Gateway and NAT Gateways configured
   - Proper routing tables set up

2. **Update Configuration**:
   - Replace placeholder VPC and subnet IDs in `environments/*.tfvars`
   - Ensure subnets are in different AZs for high availability

## Best Practices

1. **Infrastructure Reuse**: Leverages existing VPC infrastructure to avoid conflicts
2. **Cost Optimization**: Fargate Spot in dev, enhanced monitoring disabled
3. **Security**: Least-privilege IAM policies and encrypted storage
4. **Reliability**: Environment-specific configurations for different reliability needs
5. **Monitoring**: CloudWatch integration with configurable retention periods