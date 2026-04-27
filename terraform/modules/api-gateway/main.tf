# API Gateway Module - HTTP API v2 with VPC Link

# VPC Link v2 (HTTP API type)
resource "aws_apigatewayv2_vpc_link" "main" {
  name               = "${var.name_prefix}-vpc-link"
  subnet_ids         = var.private_subnet_ids
  security_group_ids = [var.apigw_vpc_link_sg_id]

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-vpc-link"
  })
}

# HTTP API v2
resource "aws_apigatewayv2_api" "main" {
  name          = "${var.name_prefix}-http-api"
  protocol_type = "HTTP"

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-http-api"
  })
}

# HTTP_PROXY integration via VPC Link → NLB
resource "aws_apigatewayv2_integration" "main" {
  api_id             = aws_apigatewayv2_api.main.id
  integration_type   = "HTTP_PROXY"
  integration_method = "ANY"
  integration_uri    = var.nlb_listener_arn
  connection_type    = "VPC_LINK"
  connection_id      = aws_apigatewayv2_vpc_link.main.id

  depends_on = [aws_apigatewayv2_vpc_link.main]
}

# Catch-all route: ANY /{proxy+}
resource "aws_apigatewayv2_route" "proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.main.id}"
}

# Root route: ANY /
resource "aws_apigatewayv2_route" "root" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "ANY /"
  target    = "integrations/${aws_apigatewayv2_integration.main.id}"
}

# Default stage with auto-deploy
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.main.id
  name        = "$default"
  auto_deploy = true

  default_route_settings {
    throttling_rate_limit  = 5
    throttling_burst_limit = 5
  }

  tags = merge(var.common_tags, {
    Name = "${var.name_prefix}-http-api-stage"
  })
}
