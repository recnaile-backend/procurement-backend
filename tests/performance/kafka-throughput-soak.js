// k6 soak test — sustained moderate load over a longer window to validate Kafka
// consumer lag stays bounded under continuous event production (approval actions,
// which each trigger a downstream event cascade across several services).
// Usage: k6 run -e BASE_URL=... kafka-throughput-soak.js
import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const TOKEN = __ENV.E2E_ACCESS_TOKEN;

export const options = {
  scenarios: {
    soak: {
      executor: "constant-vus",
      vus: 20,
      duration: "30m",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
  },
};

export default function () {
  const res = http.post(
    `${BASE_URL}/api/v1/indents/some-indent-id/approve`,
    null,
    { headers: { Authorization: `Bearer ${TOKEN}` } },
  );
  check(res, { "status is 200 or 409 (already approved is acceptable under soak)": (r) => [200, 409].includes(r.status) });
  sleep(2);
}

// Pair this script's run with a Grafana "Kafka health" dashboard watch
// (infrastructure/monitoring/grafana/dashboards/kafka-health.json) to confirm
// kafka_consumer_group_lag stays within the alert threshold throughout the soak.
