## ADDED Requirements

### Requirement: API Gateway HTTP API v2 is internet-facing
The system SHALL create an AWS API Gateway HTTP API (`aws_apigatewayv2_api`) with `protocol_type = "HTTP"` that is publicly accessible from the internet.

#### Scenario: HTTP API is created with correct protocol
- **WHEN** Terraform applies the api-gateway module
- **THEN** `aws_apigatewayv2_api` resource exists with `protocol_type = "HTTP"`

#### Scenario: HTTP API endpoint is publicly reachable
- **WHEN** a client sends an HTTP request to the API Gateway endpoint URL
- **THEN** the request is accepted by API Gateway without VPN or VPC access

### Requirement: API Gateway proxies all paths transparently
The system SHALL configure a catch-all route `ANY /{proxy+}` (plus `ANY /`) that forwards every incoming request path to the NLB target unchanged.

#### Scenario: Catch-all route handles any path
- **WHEN** a client sends a request to `<api-gateway-url>/any/nested/path`
- **THEN** API Gateway routes the request through the catch-all route without returning 404

#### Scenario: Root path is also proxied
- **WHEN** a client sends a request to `<api-gateway-url>/`
- **THEN** API Gateway routes the request through a dedicated root route

#### Scenario: All HTTP methods are forwarded
- **WHEN** a client sends GET, POST, PUT, DELETE, PATCH, or OPTIONS to any path
- **THEN** API Gateway forwards the request with the same HTTP method to the NLB

### Requirement: API Gateway forwards headers and query parameters unchanged
The system SHALL pass all request headers and query string parameters from the client to the NLB backend without modification or stripping.

#### Scenario: Custom headers are forwarded
- **WHEN** a client sends a request with custom headers (e.g., `X-Correlation-ID`)
- **THEN** the ECS application receives those headers with the same name and value

#### Scenario: Query parameters are forwarded
- **WHEN** a client sends a request with query parameters (e.g., `?page=1&size=10`)
- **THEN** the ECS application receives the same query parameters

### Requirement: API Gateway uses HTTP_PROXY integration via VPC Link
The system SHALL configure the route integration with `integration_type = "HTTP_PROXY"` and `connection_type = "VPC_LINK"` pointing to the NLB listener URI.

#### Scenario: Integration type is HTTP_PROXY
- **WHEN** the route integration is created
- **THEN** `integration_type` is `"HTTP_PROXY"`

#### Scenario: Integration uses VPC Link connection
- **WHEN** the route integration is created
- **THEN** `connection_type` is `"VPC_LINK"` and `connection_id` references the VPC Link resource

#### Scenario: Integration URI points to NLB listener
- **WHEN** the route integration is created
- **THEN** `integration_uri` is the NLB listener ARN

#### Scenario: Integration method is ANY
- **WHEN** the route integration is created
- **THEN** `integration_method` is `"ANY"` to forward all HTTP methods

### Requirement: API Gateway stage is `$default` with auto-deploy
The system SHALL create a stage named `$default` with `auto_deploy = true` so that new integrations and route changes are published automatically.

#### Scenario: Default stage exists
- **WHEN** Terraform applies the api-gateway module
- **THEN** `aws_apigatewayv2_stage` resource exists with `name = "$default"`

#### Scenario: Auto-deploy is enabled
- **WHEN** route or integration configuration changes
- **THEN** the stage is automatically redeployed without a manual deployment resource

### Requirement: API Gateway endpoint URL is exposed as Terraform output
The system SHALL expose the API Gateway invoke URL as a Terraform output `api_gateway_endpoint` for use in testing, monitoring, and future custom domain configuration.

#### Scenario: Endpoint output is available after apply
- **WHEN** Terraform apply completes successfully
- **THEN** output `api_gateway_endpoint` contains the HTTPS invoke URL of the `$default` stage

### Requirement: API Gateway has descriptive tags
The system SHALL tag the HTTP API resource with common tags and a Name tag following the `${var.name_prefix}-http-api` convention.

#### Scenario: HTTP API has Name tag
- **WHEN** HTTP API is created
- **THEN** tags include `Name = "${var.name_prefix}-http-api"`

#### Scenario: HTTP API merges common tags
- **WHEN** HTTP API is created
- **THEN** tags include all entries from `var.common_tags`
