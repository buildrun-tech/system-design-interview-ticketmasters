## Context

The current Ticketmaster system uses an Application Load Balancer (ALB) to expose the ECS service publicly on ports 80/443. The architecture needs to evolve to support API Gateway integration for better API management, authentication, rate limiting, and monitoring.

**Current Architecture:**
```
Internet → ALB (public) → ECS Service → RDS PostgreSQL
```

**Target Architecture:**
```
API Gateway → VPC Link → NLB (internal) → ECS Service → RDS PostgreSQL
```

**Constraints:**
- VPC Link only supports Network Load Balancer (NLB), not ALB
- NLB operates at Layer 4 (TCP) vs ALB at Layer 7 (HTTP/HTTPS)
- ECS service runs on Fargate with awsvpc network mode (requires IP target type)
- Existing VPC infrastructure must be preserved (subnets, route tables, etc.)

**Existing Infrastructure:**
- VPC ID: passed as variable
- Private subnets: 2 AZs for HA
- Security groups already exist: ALB SG, ECS SG, RDS SG
- ECS service: Fargate tasks on port 8080
- RDS PostgreSQL: port 5432

## Goals / Non-Goals

**Goals:**
- Enable API Gateway to privately access ECS service via VPC Link
- Maintain three-tier security group isolation (Load Balancer → ECS → RDS)
- Use NLB with IP targets for Fargate compatibility
- Keep infrastructure as code organized in existing module structure
- Support health checks and automatic target registration
- Enable cross-zone load balancing for high availability

**Non-Goals:**
- API Gateway configuration (out of scope - infrastructure team handles separately)
- TLS/SSL termination at NLB (API Gateway handles TLS termination)
- Public internet access (moving to private API Gateway integration only)
- ALB migration path (clean cutover, not gradual migration)
- Multi-region setup (single region deployment)

## Decisions

### Decision 1: Replace ALB with NLB (not run both)

**Rationale:** Clean cutover simplifies architecture and reduces costs. Running both ALB + NLB would require:
- Dual security groups
- Complex ECS service configuration accepting traffic from two sources
- Additional AWS costs for running two load balancers

**Alternatives considered:**
- **Gradual migration (ALB + NLB temporarily)**: Rejected due to complexity and cost
- **Keep ALB for direct access + add NLB**: Rejected as API Gateway becomes the only entry point

**Trade-off:** Brief downtime during migration vs operational complexity of dual load balancers.

### Decision 2: NLB Security Group uses VPC CIDR for ingress

**Rationale:** VPC Link creates ENIs in private subnets without a dedicated security group we can reference. Accepting traffic from the VPC CIDR block is secure because:
- NLB is internal (not internet-facing)
- VPC CIDR is isolated network space
- Simpler than managing specific subnet CIDR blocks

**Alternatives considered:**
- **Specific subnet CIDRs**: More restrictive but requires maintaining subnet CIDR list
- **No NLB security group**: Simpler but loses Layer 4 filtering capability

**Implementation:**
```hcl
resource "aws_security_group" "nlb" {
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.existing.cidr_block]
  }
}
```

### Decision 3: Use IP target type for NLB target group

**Rationale:** ECS Fargate with awsvpc network mode requires IP target type. Each Fargate task gets its own ENI with a private IP address that must be registered directly with the target group.

**Alternatives considered:**
- **Instance targets**: Not applicable for Fargate (only works with EC2)

**Implementation:**
```hcl
resource "aws_lb_target_group" "main" {
  target_type = "ip"
  port        = 8080
  protocol    = "TCP"
}
```

### Decision 4: TCP health checks (not HTTP)

**Rationale:** NLB operates at Layer 4 and TCP health checks are simpler and more reliable for NLB. HTTP health checks would require NLB to understand Layer 7 protocol.

**Trade-off:** TCP health checks only verify port is open, not that application is healthy. However, ECS service health checks already validate application health.

### Decision 5: Port 8080 for NLB listener and target group

**Rationale:** Maintains consistency with existing ECS container port configuration. API Gateway will forward traffic to NLB on 8080, NLB forwards to ECS on 8080.

**Why not 80/443:** API Gateway VPC Link integration doesn't require standard HTTP/HTTPS ports. Using 8080 explicitly shows this is internal API traffic, not public web traffic.

### Decision 6: Rename security group resource from `alb` to `nlb`

**Rationale:** Infrastructure code should reflect actual resources. Clearer for future developers.

**Breaking change impact:**
- Terraform will destroy old ALB security group and create new NLB security group
- Output names change: `alb_security_group_id` → `nlb_security_group_id`
- Dependent modules must update references (already tracked in root module)

**Mitigation:** All changes contained within networking and ecs modules - root module handles the wiring.

## Architecture

### Security Group Chain

```
┌─────────────────────────────────────────────────────────────┐
│                   VPC (10.0.0.0/16)                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  VPC Link (ENIs in private subnets)                        │
│       │                                                     │
│       ↓ TCP:8080                                           │
│  ┌──────────────────────┐                                  │
│  │  NLB SG (SG A)       │                                  │
│  │  ─────────────       │                                  │
│  │  Ingress:            │                                  │
│  │   • Port: 8080       │                                  │
│  │   • Proto: TCP       │                                  │
│  │   • Source: VPC CIDR │                                  │
│  │  Egress: 0.0.0.0/0   │                                  │
│  └──────────────────────┘                                  │
│       │                                                     │
│       ↓ (source_security_group: SG A)                      │
│  ┌──────────────────────┐                                  │
│  │  ECS SG (SG B)       │                                  │
│  │  ──────────────      │                                  │
│  │  Ingress:            │                                  │
│  │   • Port: 8080       │                                  │
│  │   • Proto: TCP       │                                  │
│  │   • Source: SG A     │                                  │
│  │  Egress: 0.0.0.0/0   │                                  │
│  └──────────────────────┘                                  │
│       │                                                     │
│       ↓ (source_security_group: SG B)                      │
│  ┌──────────────────────┐                                  │
│  │  RDS SG (SG C)       │                                  │
│  │  ──────────────      │                                  │
│  │  Ingress:            │                                  │
│  │   • Port: 5432       │                                  │
│  │   • Proto: TCP       │                                  │
│  │   • Source: SG B     │                                  │
│  │  Egress: (default)   │                                  │
│  └──────────────────────┘                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Module Structure

**Networking Module (`terraform/modules/networking/`):**
- Owns all security group definitions
- Outputs: `nlb_security_group_id`, `ecs_security_group_id`, `rds_security_group_id`
- Change: Rename `aws_security_group.alb` → `aws_security_group.nlb`

**ECS Module (`terraform/modules/ecs/`):**
- Owns: NLB, target group, listener, VPC Link, ECS service
- Receives: `nlb_security_group_id` from networking module
- Outputs: `nlb_dns_name`, `nlb_arn`, `vpc_link_id`
- Changes: Remove ALB, add NLB + VPC Link

**Root Module (`terraform/main.tf`):**
- Wires modules together
- Passes security group IDs between modules
- Exposes VPC Link ID for API Gateway team

## Risks / Trade-offs

### Risk 1: Downtime during ALB → NLB migration
**Impact:** Service unavailable during Terraform destroy/create cycle

**Mitigation:**
- Coordinate deployment window with stakeholders
- Deploy to staging environment first
- Consider: Create NLB first, update API Gateway, then destroy ALB (requires temporary dual setup)

### Risk 2: NLB does not support HTTP header routing
**Impact:** Cannot route based on HTTP headers, query params, or paths (Layer 4 limitation)

**Mitigation:** API Gateway handles routing logic before sending to NLB. NLB only forwards TCP traffic.

**Trade-off:** Less flexibility vs simpler infrastructure and VPC Link compatibility.

### Risk 3: VPC Link ENI placement
**Impact:** VPC Link creates ENIs in all specified subnets. If subnet CIDR changes, security group rule may need update.

**Mitigation:** Use VPC CIDR (broader range) instead of specific subnet CIDRs. VPC CIDR rarely changes.

### Risk 4: Security group reference creates dependency cycle
**Impact:** Terraform may fail if security groups reference each other incorrectly.

**Mitigation:** One-way references only:
- NLB SG → no references (uses VPC CIDR)
- ECS SG → references NLB SG
- RDS SG → references ECS SG

No circular dependencies.

### Risk 5: ECS task IPs change on deployment
**Impact:** NLB target group must deregister old IPs and register new IPs during ECS updates.

**Mitigation:** 
- `deregistration_delay = 30` allows in-flight requests to complete
- ECS service handles target registration automatically
- Health checks ensure only healthy targets receive traffic

## Migration Plan

### Phase 1: Infrastructure Preparation
1. Update Terraform code (networking + ecs modules)
2. Run `terraform plan` to verify changes
3. Review destroy/create operations (ALB destroyed, NLB created)

### Phase 2: Deployment
1. Deploy to staging environment first
2. Verify VPC Link status shows "AVAILABLE"
3. Verify NLB health checks show healthy targets
4. Test connectivity: API Gateway → VPC Link → NLB → ECS → RDS
5. Deploy to production during maintenance window

### Phase 3: Validation
1. Monitor NLB CloudWatch metrics (HealthyHostCount, UnHealthyHostCount)
2. Monitor ECS service metrics (CPU, memory, task count)
3. Test API Gateway endpoints
4. Verify security group rules block unauthorized access

### Rollback Strategy
If issues occur:
1. **Immediate rollback**: Revert Terraform code to previous version
2. Run `terraform apply` to recreate ALB
3. Update API Gateway to use ALB DNS (temporary public access)

**Rollback complexity:** High - requires destroying NLB and recreating ALB. Prefer thorough testing in staging.

## Open Questions

1. **API Gateway configuration**: Who creates the VPC Link integration in API Gateway? (Assumption: Infrastructure team handles separately)
2. **DNS/Routing**: Does anything outside Terraform reference the ALB DNS name? (Assumption: No, API Gateway will use VPC Link)
3. **Monitoring/Alerts**: Do existing CloudWatch alarms reference ALB metrics? (Needs update to NLB metrics)
4. **Cost impact**: What's the cost difference between ALB and NLB? (Assumption: Similar pricing, may vary by traffic pattern)
