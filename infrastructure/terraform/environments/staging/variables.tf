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
  default = "10.20.0.0/16"
}
variable "azs" {
  type    = list(string)
  default = ["us-east-1a", "us-east-1b"]
}
variable "public_subnet_cidrs" {
  type    = list(string)
  default = ["10.20.0.0/24", "10.20.1.0/24"]
}
variable "private_subnet_cidrs" {
  type    = list(string)
  default = ["10.20.10.0/24", "10.20.11.0/24"]
}
variable "api_domain_name" {
  type    = string
  default = "api.staging.procurement.example.com"
}
variable "acm_certificate_arn" {
  type    = string
  default = ""
}
variable "hosted_zone_id" {
  type    = string
  default = ""
}
