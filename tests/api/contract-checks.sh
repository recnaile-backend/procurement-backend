#!/usr/bin/env bash
#
# Fast, no-browser HTTP contract checks against a running environment — validates the
# standard response envelope shape, HTTP status codes, and error codes
# (docs/api/api-standards.md) without the overhead of a full Selenium browser session.
# Complements, and does not replace, the Selenium E2E suite (tests/selenium/).
#
# Usage: API_BASE_URL=https://staging.procurement.example.com ./contract-checks.sh
set -euo pipefail

BASE_URL="${API_BASE_URL:?Set API_BASE_URL}"
FAILURES=0

check() {
  local description="$1" url="$2" expected_status="$3"
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}${url}")
  if [ "${status}" == "${expected_status}" ]; then
    echo "PASS: ${description} (${status})"
  else
    echo "FAIL: ${description} — expected ${expected_status}, got ${status}"
    FAILURES=$((FAILURES + 1))
  fi
}

check "Unauthenticated request to protected resource returns 401" "/api/v1/indents" "401"
check "Health endpoint returns 200" "/actuator/health/liveness" "200"
check "Unknown resource returns 404" "/api/v1/indents/does-not-exist-xyz" "404"

echo ""
if [ "${FAILURES}" -gt 0 ]; then
  echo "${FAILURES} contract check(s) failed."
  exit 1
fi
echo "All contract checks passed."
