terraform {
  required_version = ">= 1.7"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  backend "s3" {
    # Bucket/key/region/dynamodb_table supplied via -backend-config at init time per
    # environment — never hardcoded, so the same module tree works across accounts.
  }
}

provider "aws" {
  region = var.aws_region
}

provider "aws" {
  alias  = "dr"
  region = var.dr_aws_region
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

module "vpc" {
  source               = "../../modules/vpc"
  environment          = "staging"
  vpc_cidr             = var.vpc_cidr
  azs                  = var.azs
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
}

module "eks" {
  source                      = "../../modules/eks"
  environment                 = "staging"
  vpc_id                      = module.vpc.vpc_id
  private_subnet_ids          = module.vpc.private_subnet_ids
  cluster_public_access_cidrs = var.cluster_public_access_cidrs
}

module "kafka" {
  source                = "../../modules/kafka"
  environment           = "staging"
  vpc_id                = module.vpc.vpc_id
  private_subnet_ids    = module.vpc.private_subnet_ids
  eks_security_group_id = module.eks.cluster_security_group_id
}

module "s3" {
  source             = "../../modules/s3"
  environment        = "staging"
  replication_region = var.dr_aws_region
  providers = {
    aws         = aws
    aws.replica = aws.dr
  }
}

module "redis" {
  source                = "../../modules/redis"
  environment           = "staging"
  vpc_id                = module.vpc.vpc_id
  private_subnet_ids    = module.vpc.private_subnet_ids
  eks_security_group_id = module.eks.cluster_security_group_id
  node_type             = "cache.t4g.small"
  num_cache_clusters    = 2
}


module "edge" {
  source              = "../../modules/waf-alb-route53"
  environment         = "staging"
  vpc_id              = module.vpc.vpc_id
  public_subnet_ids   = module.vpc.public_subnet_ids
  domain_name         = var.api_domain_name
  acm_certificate_arn = var.acm_certificate_arn
  hosted_zone_id      = var.hosted_zone_id
}

output "cluster_name" { value = module.eks.cluster_name }
output "redis_endpoint" { value = module.redis.primary_endpoint_address }
output "s3_bucket_name" { value = module.s3.bucket_name }
output "msk_bootstrap_brokers" { value = module.kafka.bootstrap_brokers }
