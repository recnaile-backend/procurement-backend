#!/usr/bin/env bash
#
# Operator tool for docs/runbooks/kafka-dlt-replay.md. Consumes messages from a
# topic's .dlt companion and republishes them onto the primary topic, preserving the
# original eventId so idempotent consumers safely no-op on any message that was
# actually already processed before it failed downstream.
#
# Usage:
#   ./kafka-dlt-replay.sh --topic procurement.indent.events --dry-run
#   ./kafka-dlt-replay.sh --topic procurement.indent.events
set -euo pipefail

DRY_RUN=false
TOPIC=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --topic) TOPIC="$2"; shift 2 ;;
    --dry-run) DRY_RUN=true; shift ;;
    *) echo "Unknown argument: $1"; exit 1 ;;
  esac
done

[ -z "${TOPIC}" ] && { echo "Usage: $0 --topic <topic-name> [--dry-run]"; exit 1; }

DLT_TOPIC="${TOPIC}.dlt"
BOOTSTRAP_SERVERS="${BOOTSTRAP_SERVERS:?Set BOOTSTRAP_SERVERS}"

echo "Replaying from ${DLT_TOPIC} -> ${TOPIC} (dry-run=${DRY_RUN})"

kafka-console-consumer.sh \
  --bootstrap-server "${BOOTSTRAP_SERVERS}" \
  --topic "${DLT_TOPIC}" \
  --from-beginning \
  --timeout-ms 10000 \
  --property print.headers=true \
  | while IFS= read -r line; do
      echo "Would replay: ${line}"
      if [ "${DRY_RUN}" = false ]; then
        echo "${line}" | kafka-console-producer.sh \
          --bootstrap-server "${BOOTSTRAP_SERVERS}" \
          --topic "${TOPIC}"
      fi
    done

echo "Done. Monitor consumer_processing_errors_total and DLT volume post-replay."
