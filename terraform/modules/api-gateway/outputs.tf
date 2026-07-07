# API Gateway Module Outputs

output "vpc_link_id" {
  description = "ID of the VPC Link v2"
  value       = aws_apigatewayv2_vpc_link.main.id
}

output "api_gateway_id" {
  description = "ID of the HTTP API"
  value       = aws_apigatewayv2_api.main.id
}

output "api_gateway_endpoint" {
  description = "HTTPS invoke URL of the HTTP API ($default stage)"
  value       = aws_apigatewayv2_stage.default.invoke_url
}
