# Network Architecture

## 1. Traffic Path

```text
Internet
   │
   ▼
Route 53 (DNS)
   │
   ▼
AWS WAF (managed rule groups + rate-based rule)
   │
   ▼
Application Load Balancer  (public subnet)
   │
   ▼
Spring Cloud Gateway  (EKS, private subnet, behind ALB via Ingress/NLB target group)
   │
   ▼
Domain microservices  (EKS, private subnet — never directly reachable from the internet)
```

## 2. VPC Layout

| Subnet Type | Contents | Internet Access |
|---|---|---|
| Public | ALB, NAT Gateway | Direct (via Internet Gateway) |
| Private (app) | EKS worker nodes, all 12 domain services, API Gateway pod | Outbound only, via NAT Gateway |
| Private (data) | VPC endpoints for S3/Secrets Manager/KMS/ECR; MongoDB Atlas is reached via VPC Peering or PrivateLink, not the public internet | None (endpoint-only) |

- Multi-AZ: minimum 2 AZs for public and private subnets to satisfy EKS control plane and
  ALB HA requirements.
- MongoDB Atlas: connected via **Atlas VPC Peering** or **AWS PrivateLink**, restricted by
  an Atlas IP access list scoped to the VPC CIDR — never exposed to `0.0.0.0/0`.
- Kafka: deployed either as a managed service (e.g., MSK) or self-managed StatefulSet inside
  the private app subnet; brokers are never assigned public IPs.

## 3. Kubernetes NetworkPolicy Model

Default-deny, explicit-allow:

1. Every namespace gets a default `deny-all` ingress/egress `NetworkPolicy`.
2. The API Gateway is the only workload allowed ingress from the Ingress controller.
3. Each domain service allows ingress **only** from the API Gateway and, where a documented
   synchronous dependency exists (e.g., `outward-service` → `inventory-service` stock check),
   from that specific service — never a blanket "allow all in namespace" rule.
4. Egress is allowed to: DNS (kube-dns), the service's own MongoDB Atlas endpoint, Kafka
   brokers, Redis, the OpenTelemetry Collector, and AWS service endpoints as required.
5. No service has ingress or egress rules permitting direct internet exposure.

See `infrastructure/kubernetes/base/network-policy-default-deny.yaml` and
`infrastructure/networking/` for concrete policies.

## 4. Service-to-Service Communication

- **Synchronous (REST):** only where a real-time authoritative check is required (e.g.,
  outward-service validating available stock before issuing). Kept to a documented,
  minimal set of exceptions — the default is asynchronous via Kafka.
- **Asynchronous (Kafka):** the default for all cross-domain state propagation.
- All synchronous inter-service calls use mTLS where the service mesh/ingress supports it,
  and always propagate `traceId`/`correlationId` headers.
