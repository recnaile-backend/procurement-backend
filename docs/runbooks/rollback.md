# Runbook: Rollback a Production Deployment

## Via Argo CD (standard path)

1. Identify the target Application in Argo CD (`gitops/environments/production/`).
2. `argocd app history <app-name>` to list prior synced revisions.
3. `argocd app rollback <app-name> <revision-id>` — Argo CD re-syncs the cluster to that
   revision's exact manifest state (image tag, config, replica count, etc.).
4. Verify via the standard health checks (readiness probes, Grafana dashboards, error rate)
   before closing the incident.

## Via Git (if Argo CD itself is unavailable)

1. `git revert` the offending commit in the GitOps repository (do not force-push/rewrite
   history).
2. Once Argo CD is available again, it will reconcile to the reverted state automatically
   (or trigger a manual sync).

## Notes

- Because deployments are declarative and GitOps-driven, "rollback" is always "declare the
  previous known-good state and let the reconciler apply it" — never a manual `kubectl
  edit` against a live production object.
