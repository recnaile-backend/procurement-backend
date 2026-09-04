#!/usr/bin/env bash
#
# Local-development topic bootstrap against the docker-compose single-broker Kafka.
# Uses low partition/replication counts appropriate for a laptop, unlike the
# production provisioning script (provision-topics.sh) which reads topics.yaml's
# full production settings. Kept intentionally simple/hardcoded to localhost since
# this script only ever targets the local docker-compose network.
set -euo pipefail

BROKER="localhost:9092"
TOPICS=(
  "procurement.indent.events"
  "procurement.indent.events.retry"
  "procurement.indent.events.dlt"
  "procurement.po.events"
  "procurement.po.events.retry"
  "procurement.po.events.dlt"
  "procurement.inward.events"
  "procurement.inward.events.retry"
  "procurement.inward.events.dlt"
  "procurement.inventory.events"
  "procurement.inventory.events.retry"
  "procurement.inventory.events.dlt"
  "procurement.outward.events"
  "procurement.outward.events.retry"
  "procurement.outward.events.dlt"
  "procurement.return.events"
  "procurement.return.events.retry"
  "procurement.return.events.dlt"
  "procurement.asset.events"
  "procurement.asset.events.retry"
  "procurement.asset.events.dlt"
  "procurement.notification.events"
  "procurement.notification.events.retry"
  "procurement.notification.events.dlt"
  "procurement.audit.events"
  "procurement.audit.events.retry"
  "procurement.audit.events.dlt"
  "procurement.dashboard.events"
  "procurement.dashboard.events.retry"
  "procurement.dashboard.events.dlt"
)

for topic in "${TOPICS[@]}"; do
  docker exec kafka kafka-topics.sh --bootstrap-server "${BROKER}" --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions 1 \
    --replication-factor 1
done

echo "Local topics ready. View them at http://localhost:8085 (Kafka UI)."
