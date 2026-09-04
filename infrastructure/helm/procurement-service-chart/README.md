# procurement-service-chart

Generic, reusable Helm chart for any service in the platform — the API Gateway and (once
implemented) each of the 12 domain microservices. Avoids maintaining 12+ nearly-identical
Kubernetes manifest sets by hand (MASTER PROMPT §14).

## Usage

```bash
helm upgrade --install indent-service ./infrastructure/helm/procurement-service-chart \
  -f ./infrastructure/helm/procurement-service-chart/values-production.yaml \
  --set serviceName=indent-service \
  --set namespace=production \
  --set image.repository=<account-id>.dkr.ecr.us-east-1.amazonaws.com/indent-service \
  --set image.tag=<git-sha> \
  --set aws.iamRoleArn=arn:aws:iam::<account-id>:role/indent-service-role
```

In practice, Argo CD renders this chart per service via the Application manifests in
`gitops/environments/<env>/` — this is the manual/CI-debug invocation shape.

## What This Chart Creates

- `Deployment` — non-root, read-only root filesystem, dropped capabilities, resource
  requests/limits, liveness/readiness/startup probes
- `Service` (ClusterIP — never a public LoadBalancer for a domain service)
- `ServiceAccount` with IRSA annotation
- Namespaced `Role`/`RoleBinding` scoped to the service's own ConfigMap/Secret
- `HorizontalPodAutoscaler` (CPU/memory) **or** a KEDA `ScaledObject` (Kafka-lag-driven),
  mutually exclusive via `autoscaling.keda.enabled`
- `PodDisruptionBudget`
- A `NetworkPolicy` allowing ingress only from the API Gateway plus any explicitly
  configured peer services

## Values Files

- `values.yaml` — chart defaults (safe, conservative)
- `values-dev.yaml` / `values-staging.yaml` / `values-production.yaml` — environment
  overrides layered on top via `-f`

Never put a real secret value in any of these files — `secretRefs` names existing
Kubernetes `Secret`s (populated by External Secrets Operator), it never inlines a value.
