#!/usr/bin/env bash
#
# Provisions all Kafka topics (primary + retry + dlt) defined in
# infrastructure/kafka/topics.yaml against the target cluster. Idempotent — safe to
# re-run; only creates topics that don't already exist (existing topic configuration
# is not silently mutated by this script — config changes go through a reviewed PR
# and an explicit `kafka-configs.sh --alter`, not this bootstrap script).
#
# Usage:
#   BOOTSTRAP_SERVERS=broker1:9092,broker2:9092 ./provision-topics.sh
#
set -euo pipefail

BOOTSTRAP_SERVERS="${BOOTSTRAP_SERVERS:?Set BOOTSTRAP_SERVERS}"
TOPICS_FILE="$(dirname "$0")/topics.yaml"
KAFKA_TOPICS_BIN="${KAFKA_TOPICS_BIN:-kafka-topics.sh}"

command -v yq >/dev/null 2>&1 || { echo "yq is required (https://github.com/mikefarah/yq)"; exit 1; }

create_topic() {
  local name="$1" partitions="$2" rf="$3" isr="$4" retention_ms="$5" cleanup="$6"

  if ${KAFKA_TOPICS_BIN} --bootstrap-server "${BOOTSTRAP_SERVERS}" --list | grep -qx "${name}"; then
    echo "  [skip] ${name} already exists"
    return
  fi

  echo "  [create] ${name} (partitions=${partitions}, rf=${rf}, retention.ms=${retention_ms})"
  ${KAFKA_TOPICS_BIN} --bootstrap-server "${BOOTSTRAP_SERVERS}" --create \
    --topic "${name}" \
    --partitions "${partitions}" \
    --replication-factor "${rf}" \
    --config "min.insync.replicas=${isr}" \
    --config "retention.ms=${retention_ms}" \
    --config "cleanup.policy=${cleanup}"
}

echo "Provisioning primary topics..."
default_partitions=$(yq '.defaults.partitions' "${TOPICS_FILE}")
default_rf=$(yq '.defaults.replicationFactor' "${TOPICS_FILE}")
default_isr=$(yq '.defaults.minInsyncReplicas' "${TOPICS_FILE}")
default_retention=$(yq '.defaults.retentionMs' "${TOPICS_FILE}")
default_cleanup=$(yq '.defaults.cleanupPolicy' "${TOPICS_FILE}")

retry_partitions=$(yq '.retryTopicDefaults.partitions' "${TOPICS_FILE}")
retry_rf=$(yq '.retryTopicDefaults.replicationFactor' "${TOPICS_FILE}")
retry_isr=$(yq '.retryTopicDefaults.minInsyncReplicas' "${TOPICS_FILE}")
retry_retention=$(yq '.retryTopicDefaults.retentionMs' "${TOPICS_FILE}")

dlt_partitions=$(yq '.dltTopicDefaults.partitions' "${TOPICS_FILE}")
dlt_rf=$(yq '.dltTopicDefaults.replicationFactor' "${TOPICS_FILE}")
dlt_isr=$(yq '.dltTopicDefaults.minInsyncReplicas' "${TOPICS_FILE}")
dlt_retention=$(yq '.dltTopicDefaults.retentionMs' "${TOPICS_FILE}")

topic_count=$(yq '.topics | length' "${TOPICS_FILE}")
for i in $(seq 0 $((topic_count - 1))); do
  name=$(yq ".topics[$i].name" "${TOPICS_FILE}")
  partitions=$(yq ".topics[$i].partitions // ${default_partitions}" "${TOPICS_FILE}")

  create_topic "${name}" "${partitions}" "${default_rf}" "${default_isr}" "${default_retention}" "${default_cleanup}"
  create_topic "${name}.retry" "${retry_partitions}" "${retry_rf}" "${retry_isr}" "${retry_retention}" "delete"
  create_topic "${name}.dlt" "${dlt_partitions}" "${dlt_rf}" "${dlt_isr}" "${dlt_retention}" "delete"
done

echo "Done."
