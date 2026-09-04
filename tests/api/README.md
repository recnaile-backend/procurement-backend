# API Contract Checks

Fast, no-browser HTTP checks validating the standard response envelope, status codes,
and error codes (docs/api/api-standards.md). Run in CI immediately after a service
deploys to staging, before the (slower) Selenium E2E suite runs.

```bash
API_BASE_URL=https://staging.procurement.example.com ./contract-checks.sh
```

Extend `contract-checks.sh` per-service as each domain service is implemented and its
real endpoints/status-code contracts are finalized.
