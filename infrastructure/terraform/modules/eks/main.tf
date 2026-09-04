# EKS module — private-subnet-only worker nodes, IRSA (OIDC provider) enabled so every
# workload uses least-privilege IAM roles instead of static credentials
# (architecture/security-architecture.md §3).
terraform {
  required_providers {
    aws = { source = "hashicorp/aws" }
    tls = { source = "hashicorp/tls" }
  }
}
variable "environment" { type = string }
variable "vpc_id" { type = string }
variable "private_subnet_ids" { type = list(string) }
variable "cluster_version" {
  type    = string
  default = "1.34"
}
variable "node_instance_types" {
  type    = list(string)
  default = ["m6i.large"]
}
variable "node_min_size" {
  type    = number
  default = 3
}
variable "node_max_size" {
  type    = number
  default = 10
}
variable "node_desired_size" {
  type    = number
  default = 3
}
variable "cluster_public_access_cidrs" {
  type    = list(string)
  default = []
}

resource "aws_eks_cluster" "this" {
  name     = "procurement-${var.environment}"
  role_arn = aws_iam_role.cluster.arn
  version  = var.cluster_version

  vpc_config {
    subnet_ids              = var.private_subnet_ids
    endpoint_private_access = true
    # Public endpoint access restricted to CI/operator CIDRs, not 0.0.0.0/0, in
    # production; dev/staging may allow a broader (still non-public-blanket) range
    # via a tfvars override.
    endpoint_public_access = false
    public_access_cidrs    = []
  }

  encryption_config {
    provider {
      key_arn = aws_kms_key.eks_secrets.arn
    }
    resources = ["secrets"]
  }

  tags = { Environment = var.environment }
}

resource "aws_kms_key" "eks_secrets" {
  description             = "EKS secrets envelope encryption — procurement-${var.environment}"
  deletion_window_in_days = 30
  enable_key_rotation     = true
}

resource "aws_iam_role" "cluster" {
  name = "procurement-${var.environment}-eks-cluster-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "eks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "cluster_policy" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

data "tls_certificate" "eks_oidc" {
  url = aws_eks_cluster.this.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "this" {
  url             = aws_eks_cluster.this.identity[0].oidc[0].issuer
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks_oidc.certificates[0].sha1_fingerprint]
}

resource "aws_eks_node_group" "default" {
  cluster_name    = aws_eks_cluster.this.name
  node_group_name = "procurement-${var.environment}-default"
  node_role_arn   = aws_iam_role.node.arn
  subnet_ids      = var.private_subnet_ids
  instance_types  = var.node_instance_types

  scaling_config {
    min_size     = var.node_min_size
    max_size     = var.node_max_size
    desired_size = var.node_desired_size
  }

  update_config {
    max_unavailable = 1
  }
}

resource "aws_iam_role" "node" {
  name = "procurement-${var.environment}-eks-node-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "node_worker" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
}
resource "aws_iam_role_policy_attachment" "node_cni" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}
resource "aws_iam_role_policy_attachment" "node_ecr" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

output "cluster_name" { value = aws_eks_cluster.this.name }
output "cluster_endpoint" { value = aws_eks_cluster.this.endpoint }
output "cluster_security_group_id" { value = aws_eks_cluster.this.vpc_config[0].cluster_security_group_id }
output "oidc_provider_arn" { value = aws_iam_openid_connect_provider.this.arn }
