data "aws_vpc" "this" { id = var.vpc_id }

# Kafka module — Amazon MSK path (recommended). The self-managed/Strimzi path is an
# alternative deployed via the Helm/Kubernetes layer instead of Terraform; choose one
# per environment (docs/events/kafka-architecture.md §1).
variable "environment" { type = string }
variable "private_subnet_ids" { type = list(string) }
variable "vpc_id" { type = string }
variable "eks_security_group_id" { type = string }
variable "kafka_version" {
  type    = string
  default = "3.7.x"
}
variable "broker_instance_type" {
  type    = string
  default = "kafka.m7g.large"
}
variable "number_of_broker_nodes" {
  type    = number
  default = 3
}

resource "aws_security_group" "msk" {
  name   = "procurement-${var.environment}-msk-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port       = 9098
    to_port         = 9098
    protocol        = "tcp"
    security_groups = [var.eks_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [data.aws_vpc.this.cidr_block]
  }
}

resource "aws_kms_key" "msk" {
  description             = "MSK encryption at rest — procurement-${var.environment}"
  deletion_window_in_days = 30
  enable_key_rotation     = true
  tags = { Environment = var.environment }
}

resource "aws_msk_cluster" "this" {
  cluster_name           = "procurement-${var.environment}"
  kafka_version          = var.kafka_version
  number_of_broker_nodes = var.number_of_broker_nodes

  broker_node_group_info {
    instance_type   = var.broker_instance_type
    client_subnets  = var.private_subnet_ids
    security_groups = [aws_security_group.msk.id]

    storage_info {
      ebs_storage_info {
        volume_size = 500
      }
    }
  }

  encryption_info {
    encryption_at_rest_kms_key_arn = aws_kms_key.msk.arn
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }

  client_authentication {
    sasl {
      iam = true
    }
  }

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled = true
      }
    }
  }

  tags = { Environment = var.environment }
}

output "bootstrap_brokers" { value = aws_msk_cluster.this.bootstrap_brokers_sasl_iam }
output "zookeeper_connect_string" { value = aws_msk_cluster.this.zookeeper_connect_string }
