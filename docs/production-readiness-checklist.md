# Production Readiness Checklist

## Security

- [ ] TLS terminated at ALB, valid ACM certificate, HSTS enabled at gateway
- [ ] AWS WAF attached to ALB with managed rule groups + rate-based rule
- [ ] All secrets sourced from AWS Secrets Manager via External Secrets Operator
- [ ] IAM roles use IRSA — no static credentials mounted in any pod
- [ ] Kubernetes RBAC scoped per-namespace, no unnecessary `ClusterRole` bindings
- [ ] Default-deny `NetworkPolicy` in every namespace, explicit allow-list only
- [ ] SonarQube Quality Gate passing for new code
- [ ] Trivy filesystem scan passing (no critical/high findings)
- [ ] Trivy container scan passing (no critical/high CVEs) before image promotion
- [ ] Dependency vulnerability scan (GitHub/OWASP) passing
- [ ] Secret scanning enabled and passing on every PR and push

## Reliability

- [ ] Retry, Circuit Breaker, Bulkhead, Rate Limiter configured per Resilience4j policy doc
- [ ] Request timeouts configured for every outbound call (HTTP, MongoDB Atlas, Kafka)
- [ ] Transactional outbox implemented for every event-publishing service
- [ ] Idempotent consumers implemented (unique `eventId` guard) for every event consumer
- [ ] Dead-letter topics configured and monitored for every primary topic
- [ ] MongoDB Atlas Atlas backups configured (PITR enabled) with tested restore procedure
- [ ] Disaster recovery runbook executed at least once in staging (game day)

## Observability

- [ ] Metrics exported (OpenTelemetry → Prometheus) for app, MongoDB Atlas, Kafka, Kubernetes, business KPIs
- [ ] Structured JSON logs shipped to Loki with correlation IDs present
- [ ] Distributed tracing (Tempo) covers the full request path including Kafka hops
- [ ] Grafana dashboards provisioned: platform overview, per-service, Kafka health, MongoDB Atlas health, business KPIs
- [ ] Alertmanager routes configured for page vs. notify severities
- [ ] Kafka consumer lag monitored and alerted per consumer group

## CI/CD

- [ ] PR validation pipeline: compile, unit tests, Helm lint
- [ ] SonarQube + Trivy + Gitleaks security gates passing
- [ ] Main-branch pipeline: build, container scan, push to ECR with immutable `git-sha` tag
- [ ] GitOps repository updated by CI; Argo CD (not CI) performs the Kubernetes sync
- [ ] Argo CD auto-sync enabled for dev/staging, manual approval gate for production
- [ ] Rollback procedure documented and tested (Argo CD history revert)
- [ ] Selenium E2E suite passing against staging before production promotion

## Governance

- [ ] No business logic present inside the 12 domain-service folders beyond what a
      dedicated implementation phase has added (tracked separately from this
      infrastructure baseline)
- [ ] No RabbitMQ/Amazon MQ references anywhere in the codebase or infrastructure
- [ ] No hardcoded secrets, credentials, or production URLs in source control
- [ ] All Docker images use immutable tags (`service:<git-sha>`), never `latest` alone
