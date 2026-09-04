# gitops/helm

Reserved for environment-specific Helm value overlays beyond what
`infrastructure/helm/procurement-service-chart`'s own `values-<env>.yaml` files provide
(e.g. a temporary per-environment override during an incident, or a values overlay for
an environment not yet covered by the standard three). Currently unused — the standard
dev/staging/production values in `infrastructure/helm/procurement-service-chart/`
cover all environments defined so far.
