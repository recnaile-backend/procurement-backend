# Runbook: EKS Cluster Rebuild

1. Provision a new EKS cluster via the existing Terraform modules
   (`infrastructure/terraform/environments/<env>`), unchanged from the lost cluster's
   configuration.
2. Install Argo CD into the new cluster (`gitops/argocd/install.md`).
3. Register the existing GitOps repository as the source for each environment's Argo CD
   Application (`gitops/environments/<env>/`).
4. Ensure IRSA roles/OIDC provider are (re)associated with the new cluster so External
   Secrets Operator and workload IAM permissions resolve correctly.
5. Trigger an Argo CD sync; all workloads reconcile from the GitOps repository state with
   no manual manifest authoring required.
6. Validate via the production readiness checklist before routing live traffic (update
   Route 53 / ALB target as the final cutover step).
