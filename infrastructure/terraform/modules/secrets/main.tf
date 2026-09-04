# Secrets Manager module — one secret per credential type per service, synced into the
# cluster by External Secrets Operator (architecture/security-architecture.md §5).
# Terraform provisions the Secrets Manager entries themselves; actual secret VALUES are
# set out-of-band (by a human via console/CLI, or a separate secure pipeline) — never
# committed as a Terraform variable default or tfvars value.
variable "environment" { type = string }
variable "service_names" { type = list(string) }

resource "aws_secretsmanager_secret" "mongodb_credentials" {
  for_each                = toset(var.service_names)
  name                    = "procurement/${var.environment}/${each.value}/mongodb-credentials"
  recovery_window_in_days = 30
}

resource "aws_secretsmanager_secret" "kafka_credentials" {
  for_each                = toset(var.service_names)
  name                    = "procurement/${var.environment}/${each.value}/kafka-credentials"
  recovery_window_in_days = 30
}

resource "aws_secretsmanager_secret" "jwt_signing_key" {
  count                   = var.environment == "production" ? 1 : 1
  name                    = "procurement/${var.environment}/services/auth-service/jwt-signing-key"
  recovery_window_in_days = 30
}

output "mongodb_credential_arns" {
  value = { for k, v in aws_secretsmanager_secret.mongodb_credentials : k => v.arn }
}
output "kafka_credential_arns" {
  value = { for k, v in aws_secretsmanager_secret.kafka_credentials : k => v.arn }
}
