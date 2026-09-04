# Testing Infrastructure

Selenium is the platform's specified end-to-end/acceptance testing technology
(MASTER PROMPT §17). **JUnit is intentionally not introduced** anywhere in this baseline
— see `selenium/build.gradle` for the lightweight, non-JUnit scenario runner used
instead. If unit-level testing is required later, it is added as its own deliberate,
separately-scoped decision — not smuggled in via a "just for E2E support" dependency.

## Layout

```text
tests/
├── selenium/   Full-stack UI acceptance scenarios (this is the primary test suite)
│   └── src/test/java/com/procurement/platform/e2e/
│       ├── support/   DriverFactory, ScenarioRunner, TestEnvironment (env-driven config)
│       └── flows/     ProcurementWorkflowE2E, InventoryWorkflowE2E, NegativeWorkflowsE2E
├── api/        Contract-level HTTP checks against a running environment (no browser)
└── performance/ Load/stress test scripts (k6)
```

## Running the Selenium Suite

```bash
export E2E_BASE_URL=https://staging.procurement.example.com
export E2E_EMPLOYEE_USER=... E2E_EMPLOYEE_PASSWORD=...
export E2E_PROCUREMENT_USER=... E2E_PROCUREMENT_PASSWORD=...
export E2E_TL_USER=... E2E_TL_PASSWORD=...
export E2E_MD_USER=... E2E_MD_PASSWORD=...

./gradlew test
java -cp <compiled-classpath> com.recnaile.e2e.flows.ProcurementWorkflowE2EKt
java -cp <compiled-classpath> com.recnaile.e2e.flows.InventoryWorkflowE2EKt
java -cp <compiled-classpath> com.recnaile.e2e.flows.NegativeWorkflowsE2EKt
```

CI (`.github/workflows/main-branch-build.yml`, staging stage — added once domain
services exist) runs all three against the staging environment before a release can be
promoted (see `.github/workflows/release.yml`'s `verify-staging-soak` gate).

## Credentials

Test user credentials are supplied via environment variables (populated from a
dedicated, non-production Secrets Manager entry for the staging test-user pool) —
never hardcoded in a scenario file.

## Scope Boundaries

- **selenium/** — the source of truth for full workflow acceptance (login through
  multi-service state changes), run against a real deployed environment.
- **api/** — fast, no-browser HTTP checks (status codes, response envelope shape,
  error codes) — a lighter-weight complement, not a replacement for the E2E suite.
- **performance/** — k6 load scripts validating the platform under realistic traffic
  before a production promotion, and periodically against production itself in a
  controlled window.
