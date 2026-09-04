# Installing Argo CD

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

Production hardening applied on top of the stock install (tracked as a Kustomize patch
in `gitops/argocd/production-hardening.yaml`):

- Disable the Argo CD built-in admin account once SSO/OIDC (tied into auth-service or a
  corporate IdP) is configured.
- Restrict the Argo CD API server to internal-only ingress (never expose the Argo CD UI
  itself directly to the public internet without its own auth layer).
- Namespaced RBAC per team/environment via Argo CD `AppProject`s (see `app-projects.yaml`).
