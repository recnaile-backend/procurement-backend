variable "cluster_public_access_cidrs" {
  type    = list(string)
  default = []
}
variable "dr_aws_region" {
  type    = string
  default = "us-west-2"
}
variable "vpc_cidr" {
  type    = string
  default = "10.10.0.0/16"
}
variable "azs" {
  type    = list(string)
  default = ["us-east-1a", "us-east-1b"]
}
variable "public_subnet_cidrs" {
  type    = list(string)
  default = ["10.10.0.0/24", "10.10.1.0/24"]
}
variable "private_subnet_cidrs" {
  type    = list(string)
  default = ["10.10.10.0/24", "10.10.11.0/24"]
}
variable "api_domain_name" {
  type    = string
  default = "api.dev.procurement.example.com"
}
variable "acm_certificate_arn" {
  type    = string
  default = "" # supplied via -var or dev.tfvars, never hardcoded to a real ARN here
}
variable "hosted_zone_id" {
  type    = string
  default = "" # supplied via -var or dev.tfvars
}
