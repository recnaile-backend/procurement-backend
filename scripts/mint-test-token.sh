#!/usr/bin/env bash
#
# Mints a short-lived JWT access token against a non-production auth-service instance
# for use by the Selenium E2E suite (test credentials only — never production
# credentials, per docs/production-readiness-checklist.md governance section).
set -euo pipefail

AUTH_URL="${AUTH_SERVICE_URL:?Set AUTH_SERVICE_URL}"
USERNAME="${E2E_EMPLOYEE_USER:?Set E2E_EMPLOYEE_USER}"
PASSWORD="${E2E_EMPLOYEE_PASSWORD:?Set E2E_EMPLOYEE_PASSWORD}"

curl -s -X POST "${AUTH_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}" \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['data']['accessToken'])"
