# Terraform — AWS Infrastructure

## Layout

```text
terraform/
├── modules/
│   ├── vpc/                  Public/private subnets across 2-3 AZs, NAT Gateway
│   ├── eks/                  EKS cluster + node group, IRSA/OIDC provider, KMS-encrypted secrets
│   ├── kafka/                Amazon MSK (recommended managed path)
│   ├── s3/                   Document storage bucket — versioned, encrypted, replicated (prod)
│   ├── secrets/              Secrets Manager entries (values set out-of-band, never in tfvars)
│   └── waf-alb-route53/      Public edge: WAF, ALB, Route 53
└── environments/
    ├── dev/
    ├── staging/
    └── production/
```

Each environment directory wires the shared modules together with environment-specific
variables — no module contains environment-specific values itself.

## Usage

```bash
cd environments/production
terraform init -backend-config=backend.hcl   # backend.hcl is NOT committed — supplies
                                              # the S3 state bucket/key/DynamoDB lock table
terraform plan  -var-file=production.tfvars  # production.tfvars is NOT committed —
                                              # supplies acm_certificate_arn, hosted_zone_id, etc.
terraform apply -var-file=production.tfvars
```

## What Is Never Committed

- `*.tfvars` (only `*.tfvars.example` templates, if added)
- `backend.hcl` remote-state configuration
- Any real ACM certificate ARN, Route 53 hosted zone ID, or account ID beyond what is
  needed for illustration

## Self-Managed Kafka Alternative

If MSK is not the chosen path for a given environment, the `modules/kafka` module is
simply not invoked from that environment's `main.tf`, and Kafka is instead deployed via
the Strimzi Operator Helm chart directly onto EKS (see `docs/events/kafka-architecture.md` §1).
