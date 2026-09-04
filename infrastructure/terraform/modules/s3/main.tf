# =============================================================================
# S3 MODULE
# =============================================================================
#
# Purpose:
#   Stores invoices/documents.
#
# Architecture:
#   - MongoDB stores object keys and metadata only.
#   - S3 stores the actual documents.
#   - KMS encrypts objects at rest.
#   - Versioning is enabled.
#   - Non-current versions are transitioned to cheaper storage.
#   - Production supports cross-region replication (DR).
#
# Environments:
#   DEV        -> Primary S3 bucket only
#   STAGING    -> Primary S3 bucket only
#   PRODUCTION -> Primary + DR replica bucket
#
# =============================================================================


# -----------------------------------------------------------------------------
# Terraform / Provider Requirements
# -----------------------------------------------------------------------------

terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"

      configuration_aliases = [
        aws.replica
      ]
    }
  }
}


# -----------------------------------------------------------------------------
# Variables
# -----------------------------------------------------------------------------

variable "environment" {
  description = "Deployment environment"
  type        = string

  validation {
    condition = contains(
      ["dev", "staging", "production"],
      var.environment
    )

    error_message = "Environment must be dev, staging, or production."
  }
}

variable "replication_region" {
  description = "AWS region used for production S3 disaster recovery replication"
  type        = string
}


# =============================================================================
# KMS
# =============================================================================

resource "aws_kms_key" "documents" {
  description             = "S3 document encryption - procurement-${var.environment}"
  deletion_window_in_days = 30
  enable_key_rotation     = true

  tags = {
    Name        = "procurement-${var.environment}-documents"
    Environment = var.environment
    ManagedBy   = "Terraform"
    Purpose     = "Document Encryption"
  }
}


resource "aws_kms_alias" "documents" {
  name          = "alias/procurement-${var.environment}-documents"
  target_key_id = aws_kms_key.documents.key_id
}


# =============================================================================
# PRIMARY S3 BUCKET
# =============================================================================

resource "aws_s3_bucket" "documents" {
  bucket = "procurement-${var.environment}-documents"

  tags = {
    Name        = "procurement-${var.environment}-documents"
    Environment = var.environment
    ManagedBy   = "Terraform"
    Purpose     = "Invoice and document storage"
  }
}


# =============================================================================
# PRIMARY BUCKET VERSIONING
# =============================================================================

resource "aws_s3_bucket_versioning" "documents" {
  bucket = aws_s3_bucket.documents.id

  versioning_configuration {
    status = "Enabled"
  }
}


# =============================================================================
# PRIMARY BUCKET ENCRYPTION
# =============================================================================

resource "aws_s3_bucket_server_side_encryption_configuration" "documents" {
  bucket = aws_s3_bucket.documents.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.documents.arn
    }

    bucket_key_enabled = true
  }
}


# =============================================================================
# PRIMARY BUCKET PUBLIC ACCESS BLOCK
# =============================================================================

resource "aws_s3_bucket_public_access_block" "documents" {
  bucket = aws_s3_bucket.documents.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}


# =============================================================================
# PRIMARY BUCKET LIFECYCLE
# =============================================================================
#
# Non-current object versions:
#
#   After 30 days -> STANDARD_IA
#   After 90 days -> GLACIER
#   After 2555 days (~7 years) -> Delete
#
# =============================================================================

resource "aws_s3_bucket_lifecycle_configuration" "documents" {
  bucket = aws_s3_bucket.documents.id

  rule {
    id     = "transition-noncurrent-versions"
    status = "Enabled"

    noncurrent_version_transition {
      noncurrent_days = 30
      storage_class   = "STANDARD_IA"
    }

    noncurrent_version_transition {
      noncurrent_days = 90
      storage_class   = "GLACIER"
    }

    noncurrent_version_expiration {
      noncurrent_days = 2555
    }
  }
}


# =============================================================================
# PRODUCTION DR REPLICA BUCKET
# =============================================================================
#
# Only created for production.
#
# Example:
#
# Primary:
#   AWS region: primary region
#   Bucket: procurement-production-documents
#
# DR:
#   AWS region: replication_region
#   Bucket: procurement-production-documents-dr
#
# =============================================================================

resource "aws_s3_bucket" "documents_replica" {
  count = var.environment == "production" ? 1 : 0

  provider = aws.replica

  bucket = "procurement-production-documents-dr"

  tags = {
    Name        = "procurement-production-documents-dr"
    Environment = "production"
    ManagedBy   = "Terraform"
    Purpose     = "Cross-region disaster recovery"
  }
}


# =============================================================================
# REPLICA BUCKET VERSIONING
# =============================================================================

resource "aws_s3_bucket_versioning" "documents_replica" {
  count = var.environment == "production" ? 1 : 0

  provider = aws.replica

  bucket = aws_s3_bucket.documents_replica[0].id

  versioning_configuration {
    status = "Enabled"
  }
}


# =============================================================================
# REPLICA BUCKET ENCRYPTION
# =============================================================================
#
# Creates a KMS key in the replica region for DR objects.
#
# =============================================================================

resource "aws_kms_key" "documents_replica" {
  count = var.environment == "production" ? 1 : 0

  provider = aws.replica

  description             = "S3 DR document encryption - procurement-production"
  deletion_window_in_days = 30
  enable_key_rotation     = true

  tags = {
    Name        = "procurement-production-documents-dr"
    Environment = "production"
    ManagedBy   = "Terraform"
    Purpose     = "DR Document Encryption"
  }
}


resource "aws_kms_alias" "documents_replica" {
  count = var.environment == "production" ? 1 : 0

  provider = aws.replica

  name          = "alias/procurement-production-documents-dr"
  target_key_id = aws_kms_key.documents_replica[0].key_id
}


resource "aws_s3_bucket_server_side_encryption_configuration" "documents_replica" {
  count = var.environment == "production" ? 1 : 0

  provider = aws.replica

  bucket = aws_s3_bucket.documents_replica[0].id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.documents_replica[0].arn
    }

    bucket_key_enabled = true
  }
}


# =============================================================================
# REPLICA PUBLIC ACCESS BLOCK
# =============================================================================

resource "aws_s3_bucket_public_access_block" "documents_replica" {
  count = var.environment == "production" ? 1 : 0

  provider = aws.replica

  bucket = aws_s3_bucket.documents_replica[0].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}


# =============================================================================
# REPLICA LIFECYCLE
# =============================================================================

resource "aws_s3_bucket_lifecycle_configuration" "documents_replica" {
  count = var.environment == "production" ? 1 : 0

  provider = aws.replica

  bucket = aws_s3_bucket.documents_replica[0].id

  rule {
    id     = "dr-noncurrent-version-management"
    status = "Enabled"

    noncurrent_version_transition {
      noncurrent_days = 30
      storage_class   = "STANDARD_IA"
    }

    noncurrent_version_transition {
      noncurrent_days = 90
      storage_class   = "GLACIER"
    }

    noncurrent_version_expiration {
      noncurrent_days = 2555
    }
  }
}


# =============================================================================
# S3 REPLICATION IAM ROLE
# =============================================================================

resource "aws_iam_role" "replication" {
  count = var.environment == "production" ? 1 : 0

  name = "procurement-production-s3-replication-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Effect = "Allow"

        Principal = {
          Service = "s3.amazonaws.com"
        }

        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = {
    Environment = "production"
    ManagedBy   = "Terraform"
    Purpose     = "S3 Cross Region Replication"
  }
}


# =============================================================================
# S3 REPLICATION IAM POLICY
# =============================================================================
#
# Permissions:
#
# Source bucket:
#   - Read object versions
#   - Read replication configuration
#
# Destination bucket:
#   - Replicate objects
#   - Replicate deletes
#   - Replicate tags
#
# KMS:
#   - Decrypt source objects
#   - Encrypt destination objects
#
# =============================================================================

resource "aws_iam_role_policy" "replication" {
  count = var.environment == "production" ? 1 : 0

  role = aws_iam_role.replication[0].id

  policy = jsonencode({
    Version = "2012-10-17"

    Statement = [

      # -----------------------------------------------------------------------
      # Source S3 permissions
      # -----------------------------------------------------------------------

      {
        Effect = "Allow"

        Action = [
          "s3:GetObjectVersion",
          "s3:GetObjectVersionAcl",
          "s3:GetObjectVersionForReplication",
          "s3:GetObjectLegalHold",
          "s3:GetObjectRetention",
          "s3:GetObjectVersionTagging"
        ]

        Resource = "${aws_s3_bucket.documents.arn}/*"
      },

      {
        Effect = "Allow"

        Action = [
          "s3:ListBucket",
          "s3:GetReplicationConfiguration"
        ]

        Resource = aws_s3_bucket.documents.arn
      },

      # -----------------------------------------------------------------------
      # Destination S3 permissions
      # -----------------------------------------------------------------------

      {
        Effect = "Allow"

        Action = [
          "s3:ReplicateObject",
          "s3:ReplicateDelete",
          "s3:ReplicateTags",
          "s3:ObjectOwnerOverrideToBucketOwner"
        ]

        Resource = "${aws_s3_bucket.documents_replica[0].arn}/*"
      },

      # -----------------------------------------------------------------------
      # Source KMS permissions
      # -----------------------------------------------------------------------

      {
        Effect = "Allow"

        Action = [
          "kms:Decrypt"
        ]

        Resource = aws_kms_key.documents.arn
      },

      # -----------------------------------------------------------------------
      # Destination KMS permissions
      # -----------------------------------------------------------------------

      {
        Effect = "Allow"

        Action = [
          "kms:Encrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*"
        ]

        Resource = aws_kms_key.documents_replica[0].arn
      }
    ]
  })
}


# =============================================================================
# S3 REPLICATION CONFIGURATION
# =============================================================================

resource "aws_s3_bucket_replication_configuration" "documents" {
  count = var.environment == "production" ? 1 : 0

  bucket = aws_s3_bucket.documents.id

  role = aws_iam_role.replication[0].arn

  depends_on = [
    aws_s3_bucket_versioning.documents,
    aws_s3_bucket_versioning.documents_replica,
    aws_iam_role_policy.replication
  ]

  rule {
    id     = "cross-region-dr"
    status = "Enabled"

    filter {}

    destination {
      bucket = aws_s3_bucket.documents_replica[0].arn

      storage_class = "STANDARD"

      encryption_configuration {
        replica_kms_key_id = aws_kms_key.documents_replica[0].arn
      }
    }
  }
}


# =============================================================================
# OUTPUTS
# =============================================================================

output "bucket_name" {
  description = "Primary document S3 bucket name"
  value       = aws_s3_bucket.documents.bucket
}


output "bucket_arn" {
  description = "Primary document S3 bucket ARN"
  value       = aws_s3_bucket.documents.arn
}


output "kms_key_arn" {
  description = "Primary document KMS key ARN"
  value       = aws_kms_key.documents.arn
}


output "replica_bucket_name" {
  description = "Production DR S3 bucket name"
  value = var.environment == "production" ? (
    aws_s3_bucket.documents_replica[0].bucket
  ) : null
}


output "replica_bucket_arn" {
  description = "Production DR S3 bucket ARN"
  value = var.environment == "production" ? (
    aws_s3_bucket.documents_replica[0].arn
  ) : null
}


output "replica_kms_key_arn" {
  description = "Production DR KMS key ARN"
  value = var.environment == "production" ? (
    aws_kms_key.documents_replica[0].arn
  ) : null
}