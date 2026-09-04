# Security Scan Policy

## Required gates

- SonarQube: Java code quality, SAST and coverage quality gate for trusted PRs and branch pushes.
- Trivy filesystem: CRITICAL/HIGH vulnerabilities, secrets and IaC misconfiguration.
- Trivy container: CRITICAL/HIGH vulnerabilities, secrets and configuration in the API Gateway image.
- Gitleaks: Git-history secret detection without requiring a GitHub API token.

## Dependabot

Dependabot PRs intentionally skip SonarQube because repository secrets are not exposed to Dependabot execution. Trivy and Gitleaks remain active and the Security Summary remains the required aggregate gate.

## Accepted Trivy exception

`AWS-0053` is explicitly ignored because the application architecture requires a public internet-facing Application Load Balancer as the edge. The ALB is protected by AWS WAF and the application workloads are private.

Any new Trivy exception requires a documented architectural reason and security-owner review. Do not suppress findings merely to make CI green.
