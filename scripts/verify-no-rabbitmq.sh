#!/usr/bin/env bash
#
# CI/pre-commit guard enforcing MASTER PROMPT §48-1/§48-2: RabbitMQ must never appear
# anywhere in this codebase. Run from the repo root.
set -euo pipefail

MATCHES=$(grep -ril --exclude-dir=.git --exclude-dir=build --exclude-dir=.gradle \
  -e "rabbitmq" -e "amqp" -e "amazon mq" -e "amazonmq" . || true)

if [ -n "${MATCHES}" ]; then
  echo "FAIL: RabbitMQ/Amazon MQ references found (forbidden — Kafka is the only messaging technology):"
  echo "${MATCHES}"
  exit 1
fi

echo "PASS: no RabbitMQ/Amazon MQ references found."
