# Metrics Catalogue

All metrics exported in Prometheus exposition format via the OpenTelemetry Collector's
Prometheus exporter (or Micrometer's Prometheus registry for JVM-native metrics),
scraped per the `ServiceMonitor`/scrape-config in `infrastructure/monitoring/`.

## Application (per service)

| Metric | Type | Notes |
|---|---|---|
| `http_server_requests_seconds` | histogram | Tagged by `service`, `route`, `method`, `status`; source for P50/P95/P99 latency and error-rate panels |
| `http_server_requests_errors_total` | counter | 5xx responses |
| `jvm_memory_used_bytes` | gauge | Heap/non-heap |
| `process_cpu_usage` | gauge | |
| `jvm_gc_pause_seconds` | histogram | GC pause duration |
| `executor_pool_size_threads` / `executor_active_threads` | gauge | Thread pool saturation |

## MongoDB (per service database)

| Metric | Type | Notes |
|---|---|---|
| `mongodb_driver_pool_checkedout` / `mongodb_driver_pool_size` | gauge | Connection pool usage |
| `mongodb_operation_duration_seconds` | histogram | Query latency by operation type |
| `mongodb_operation_errors_total` | counter | |
| `mongodb_operation_timeouts_total` | counter | |

## Kafka

| Metric | Type | Notes |
|---|---|---|
| `kafka_consumer_group_lag` | gauge | Per group/partition — the single most important Kafka health signal |
| `kafka_producer_errors_total` | counter | |
| `kafka_consumer_errors_total` | counter | |
| `kafka_records_consumed_total` / `kafka_records_produced_total` | counter | records/sec derived via `rate()` |
| `kafka_partition_under_replicated` | gauge | Partition/broker health |
| `kafka_consumer_group_rebalances_total` | counter | Frequent rebalances indicate consumer instability |
| `kafka_dlt_messages_total` | counter | Per topic — DLT volume, directly alertable |

## Kubernetes (cluster-wide, via kube-state-metrics + node-exporter)

| Metric | Type | Notes |
|---|---|---|
| `container_cpu_usage_seconds_total` / `container_memory_working_set_bytes` | counter/gauge | vs. requests/limits for right-sizing |
| `kube_pod_status_phase` | gauge | |
| `kube_pod_container_status_restarts_total` | counter | Crash-loop detection |
| `kube_node_status_condition` | gauge | Node health |
| `container_network_receive_bytes_total` / `..._transmit_..._total` | counter | |

## Business (dashboard-service aggregates, sourced from domain events)

| Metric | Notes |
|---|---|
| `business_indents_submitted_total` | |
| `business_indents_approved_total` / `business_indents_rejected_total` | |
| `business_indents_pending_approval` | gauge, current queue depth |
| `business_pos_created_total` / `business_pos_issued_total` | |
| `business_goods_received_total` / `business_goods_rejected_total` | |
| `business_stock_movements_total` | tagged by direction (in/out) |
| `business_low_stock_items` | gauge |
| `business_outward_requests_total` | |
| `business_returns_total` | |
| `business_assets_active` | gauge |
| `business_sla_breaches_total` | |

These business metrics are derived by dashboard-service's event consumers, not scraped
directly from every service — keeping business-KPI computation centralized and consistent.
