# tests/e2e

Reserved for full-system, multi-day/scheduled end-to-end validation (e.g. an overnight
job that walks an indent all the way through PO issuance, GRN, and stock update against
a long-lived staging environment) — distinct from `tests/selenium/`, which holds the
per-PR/per-release UI acceptance scenarios.

Currently empty; populate as these longer-running scenarios are needed. Any scripts
added here follow the same rule as the rest of `tests/`: Selenium for browser-driven
steps, no JUnit (MASTER PROMPT §17/§20).
