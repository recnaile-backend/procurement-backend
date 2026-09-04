# AWS Multi-Cluster Deployment Standard

## Target topology

One AWS account contains three isolated EKS clusters:

```text
AWS Account
├── procurement-dev
├── procurement-staging
└── procurement-production
```

Each cluster has its own VPC and environment-scoped managed resources. Production uses a
separate namespace and manual Argo CD synchronization policy. The Terraform state is stored
in a versioned, encrypted S3 bucket with DynamoDB locking.

## Entry points

| Environment | Script | Cluster |
|---|---|---|
| Dev | `scripts/deploy-dev.ps1` | `procurement-dev` |
| Staging | `scripts/deploy-staging.ps1` | `procurement-staging` |
| Production | `scripts/deploy-production.ps1` | `procurement-production` |

Do not run a production script from an unreviewed branch. In GitHub Actions, use a protected
`production` Environment and require an authorized approval.

## Prerequisites

1. AWS CLI v2 authenticated to the intended single AWS account.
2. Terraform >= 1.7.
3. kubectl.
4. Helm 3.
5. Docker Desktop when publishing images.
6. A public Route 53 hosted zone.
7. ACM certificate covering each API hostname in the selected region.
8. Git remote URL configured when `-DeployApplications` is used.

## Required variables

Never commit real values into Terraform files or `*.tfvars`.

```powershell
$env:AWS_REGION = "us-east-1"
$env:DR_AWS_REGION = "us-west-2"
$env:ACM_CERTIFICATE_ARN = "..."
$env:HOSTED_ZONE_ID = "..."
$env:GITOPS_REPO_URL = "https://github.com/YOUR_ORG/YOUR_REPO.git"
```

The scripts automatically restrict the EKS public API endpoint to the current operator IP
address (`/32`). For GitHub Actions, the workflow runner IP is not stable, so a production
MNC setup should use a self-hosted runner inside the AWS network or a controlled network path
rather than broadening the cluster API endpoint to `0.0.0.0/0`.

## Deployment sequence

```text
PowerShell script
      |
      +--> Bootstrap encrypted Terraform state
      |
      +--> Terraform init / plan / apply
      |
      +--> VPC + private EKS + MSK + Redis + S3/KMS + Secrets Manager + edge
      |
      +--> Configure kubectl
      |
      +--> Install Argo CD
      |
      +--> Create ECR repositories
      |
      +--> Build/push implemented images (when requested)
      |
      +--> Register Argo CD Applications
```

GitHub Actions remains the CI system. It should build, test, scan and publish images; Argo CD
is the Kubernetes reconciliation engine.

## Important baseline limitation

The 12 business services are reserved/unimplemented in this repository. Therefore the
automation intentionally does not fabricate business-service images. Their ECR repositories
are created so implementation teams can add Dockerfiles and image pipelines without changing
the AWS topology.

The API Gateway is the only implemented application image in the current baseline.

## Terraform state

The first deployment creates, if missing:

- S3 state bucket: `procurement-platform-tfstate-<AWS_ACCOUNT_ID>`
- DynamoDB lock table: `procurement-platform-tf-locks`

The state bucket is versioned, encrypted and blocked from public access.

## GitHub Actions / MNC governance

Recommended repository protections:

- no direct pushes to `main` or `develop`
- PR required
- CODEOWNERS approval required
- CI checks required
- security scan required
- production GitHub Environment approval required
- immutable image tags based on Git SHA
- AWS authentication through GitHub OIDC, not long-lived access keys
- Terraform state stored remotely
- production Argo CD synchronization manual and audited
