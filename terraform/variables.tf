variable "existing_user_pool_id" {
  type        = string
  default     = "us-east-1_tcxYPcLLV"
  description = "ID do Cognito User Pool existente. Deixe vazio para criar um novo."
}

variable "existing_user_pool_arn" {
  type        = string
  default     = "arn:aws:cognito-idp:us-east-1:420606830531:userpool/us-east-1_tcxYPcLLV"
  description = "ARN do Cognito User Pool existente, necessário para Authorizer. Deixe vazio se estiver criando novo."
}

variable "cognito_app_client_name" {
  type        = string
  default     = "todo-app-client"
  description = "Nome do Cognito App Client"
}

variable "cognito_domain_prefix" {
  type        = string
  default     = "fabricio-todo-api"
  description = "Prefixo do domínio do Cognito. Use só a parte antes de '.auth.region.amazoncognito.com'"
}
