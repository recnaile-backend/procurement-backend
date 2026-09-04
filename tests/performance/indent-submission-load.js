// k6 load test — indent submission under realistic concurrent load.
// Usage: k6 run -e BASE_URL=https://staging.procurement.example.com indent-submission-load.js
import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const TOKEN = __ENV.E2E_ACCESS_TOKEN; // short-lived test token, never hardcoded

export const options = {
  scenarios: {
    steady_load: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "2m", target: 50 },
        { duration: "5m", target: 50 },
        { duration: "2m", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<800", "p(99)<1500"], // aligned with the P95 latency alert
    http_req_failed: ["rate<0.01"],
  },
};

export default function () {
  const payload = JSON.stringify({
    departmentId: "DEPT-001",
    projectId: "PROJ-001",
    items: [{ itemId: "ITEM-001", quantity: 5 }],
  });

  const res = http.post(`${BASE_URL}/api/v1/indents`, payload, {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${TOKEN}`,
    },
  });

  check(res, {
    "status is 201": (r) => r.status === 201,
    "response has success envelope": (r) => JSON.parse(r.body).success === true,
  });

  sleep(1);
}
