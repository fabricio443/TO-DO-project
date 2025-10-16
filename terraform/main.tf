terraform {
  backend "s3" {
    bucket  = "fabricio-todo-tfstate"
    key     = "terraform/state/todo-app.tfstate"
    region  = "us-east-1"
    encrypt = true
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}

# -------------------------------
# IAM Role para Lambda
# -------------------------------
resource "aws_iam_role" "lambda_role" {
  name = "lambda_dynamodb_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Action = "sts:AssumeRole",
      Effect = "Allow",
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "lambda_policy" {
  name = "lambda_dynamodb_policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "dynamodb:PutItem",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "dynamodb:Query",
          "dynamodb:Scan",
          "dynamodb:DescribeTable",
          "dynamodb:ListTables"
        ],
        Resource = aws_dynamodb_table.todo_table.arn
      },
      {
        Effect = "Allow",
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ],
        Resource = "*"
      }
    ]
  })
}

# -------------------------------
# DynamoDB Table
# -------------------------------
resource "aws_dynamodb_table" "todo_table" {
  name         = "todo_lists"
  billing_mode = "PAY_PER_REQUEST"

  hash_key  = "pk"
  range_key = "sk"

  attribute {
    name = "pk"
    type = "S"
  }

  attribute {
    name = "sk"
    type = "S"
  }

  tags = {
    Project = "TodoList"
  }
}

# -------------------------------
# API Gateway REST
# -------------------------------
resource "aws_api_gateway_rest_api" "todo_api" {
  name        = "todo-rest-api"
  description = "API REST para TodoList"
}

# -------------------------------
# Lambda Functions
# -------------------------------
locals {
  lambda_functions = [
    { name = "create-todolist", handler = "org.example.list.CreateTodoListHandler::handleRequest" },
    { name = "get-todolist",    handler = "org.example.list.GetTodoListsHandler::handleRequest" },
    { name = "update-todolist", handler = "org.example.list.UpdateTodoListHandler::handleRequest" },
    { name = "create-item",     handler = "org.example.item.CreateTodoItemHandler::handleRequest" },
    { name = "get-items",       handler = "org.example.item.GetTodoItemHandler::handleRequest" },
    { name = "update-item",     handler = "org.example.item.UpdateTodoItemHandler::handleRequest" },
  ]
}

resource "aws_lambda_function" "lambda_functions" {
  for_each     = { for f in local.lambda_functions : f.name => f }
  function_name = each.value.name
  role          = aws_iam_role.lambda_role.arn
  handler       = each.value.handler
  runtime       = "java17"
  memory_size   = 512
  timeout       = 10

  environment {
    variables = {
      TABLE_NAME = aws_dynamodb_table.todo_table.name
    }
  }

  filename         = "${path.module}/../target/todo-lambda-1.0-SNAPSHOT.jar"
  source_code_hash = filebase64sha256("${path.module}/../target/todo-lambda-1.0-SNAPSHOT.jar")

  depends_on = [
    aws_dynamodb_table.todo_table
  ]
}


# -------------------------------
# API Gateway Resources
# -------------------------------
resource "aws_api_gateway_resource" "todos" {
  rest_api_id = aws_api_gateway_rest_api.todo_api.id
  parent_id   = aws_api_gateway_rest_api.todo_api.root_resource_id
  path_part   = "todos"
}

resource "aws_api_gateway_resource" "lists" {
  rest_api_id = aws_api_gateway_rest_api.todo_api.id
  parent_id   = aws_api_gateway_rest_api.todo_api.root_resource_id
  path_part   = "lists"
}

resource "aws_api_gateway_resource" "items" {
  rest_api_id = aws_api_gateway_rest_api.todo_api.id
  parent_id   = aws_api_gateway_rest_api.todo_api.root_resource_id
  path_part   = "items"
}

# -------------------------------
# Cognito User Pool
# -------------------------------
resource "aws_cognito_user_pool" "todo_user_pool" {
  count = var.existing_user_pool_id != "" ? 0 : 1

  name                     = "todo-user-pool"
  auto_verified_attributes = ["email"]

  schema {
    name               = "email"
    required           = true
    attribute_data_type = "String"
    mutable            = true
  }

  password_policy {
    minimum_length    = 8
    require_lowercase = true
    require_numbers   = true
    require_symbols   = false
    require_uppercase = true
  }
}

# -------------------------------
# Cognito App Client
# -------------------------------
resource "aws_cognito_user_pool_client" "todo_app_client" {
  count = var.existing_user_pool_id != "" ? 0 : 1  # só cria se não existir

  name         = var.cognito_app_client_name
  user_pool_id = aws_cognito_user_pool.todo_user_pool[0].id

  explicit_auth_flows = [
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH"
  ]

  generate_secret = false
}

# -------------------------------
# Cognito Domain
# -------------------------------
resource "aws_cognito_user_pool_domain" "todo_domain" {
  count = var.existing_user_pool_id != "" ? 0 : 1

  domain       = var.cognito_domain_prefix
  user_pool_id = aws_cognito_user_pool.todo_user_pool[0].id
}

# -------------------------------
# Cognito Authorizer
# -------------------------------
resource "aws_api_gateway_authorizer" "cognito_authorizer" {
  name            = "cognito-authorizer"
  rest_api_id     = aws_api_gateway_rest_api.todo_api.id
  type            = "COGNITO_USER_POOLS"
  provider_arns   = [var.existing_user_pool_id != "" ? var.existing_user_pool_arn : aws_cognito_user_pool.todo_user_pool[0].arn]
  identity_source = "method.request.header.Authorization"
}

# -------------------------------
# API Gateway Methods
# -------------------------------
locals {
  api_methods = [
    { resource = aws_api_gateway_resource.todos.id, method = "POST", lambda = "create-todolist" },
    { resource = aws_api_gateway_resource.todos.id, method = "PUT",  lambda = "update-todolist" },
    { resource = aws_api_gateway_resource.lists.id, method = "GET",  lambda = "get-todolist" },
    { resource = aws_api_gateway_resource.items.id, method = "POST", lambda = "create-item" },
    { resource = aws_api_gateway_resource.items.id, method = "GET",  lambda = "get-items" },
    { resource = aws_api_gateway_resource.items.id, method = "PUT",  lambda = "update-item" },
  ]
}

resource "aws_api_gateway_method" "api_methods" {
  for_each     = { for m in local.api_methods : "${m.resource}-${m.method}" => m }
  rest_api_id  = aws_api_gateway_rest_api.todo_api.id
  resource_id  = each.value.resource
  http_method  = each.value.method
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
}

# -------------------------------
# API Gateway Integrations
# -------------------------------
resource "aws_api_gateway_integration" "api_integrations" {
  for_each                = { for m in local.api_methods : "${m.resource}-${m.method}" => m }
  rest_api_id             = aws_api_gateway_rest_api.todo_api.id
  resource_id             = each.value.resource
  http_method             = aws_api_gateway_method.api_methods[each.key].http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.lambda_functions[each.value.lambda].invoke_arn
}

# -------------------------------
# Lambda Permissions
# -------------------------------
resource "aws_lambda_permission" "api_permissions" {
  for_each       = { for m in local.api_methods : "${m.resource}-${m.method}" => m }
  statement_id   = "AllowAPIGatewayInvoke-${each.value.lambda}"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.lambda_functions[each.value.lambda].function_name
  principal      = "apigateway.amazonaws.com"
  source_arn     = "${aws_api_gateway_rest_api.todo_api.execution_arn}/*/*"
}

# -------------------------------
# Deployment e Stage
# -------------------------------
resource "aws_api_gateway_deployment" "todo_deployment" {
  rest_api_id = aws_api_gateway_rest_api.todo_api.id

  depends_on = [
    aws_api_gateway_integration.api_integrations,
    aws_api_gateway_authorizer.cognito_authorizer
  ]
}

resource "aws_api_gateway_stage" "todo_stage" {
  rest_api_id   = aws_api_gateway_rest_api.todo_api.id
  deployment_id = aws_api_gateway_deployment.todo_deployment.id
  stage_name    = "dev"
}
