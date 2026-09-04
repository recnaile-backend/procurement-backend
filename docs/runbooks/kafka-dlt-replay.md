# Runbook: Kafka Dead-Letter Topic Replay

## When to Use

A dead-letter topic (`<topic>.dlt`) has accumulated messages that failed processing after
exhausting retries, and the root cause has been identified and fixed (bug fix deployed, or
external dependency restored).

## Steps

1. **Investigate first.** Never replay blindly. Consume a sample from the `.dlt` topic and
   inspect the `failureReason`/`failureCount` headers to confirm the root cause is actually
   resolved.
2. **Confirm the fix is deployed** to the consuming service in the target environment.
3. **Dry run:** run the replay tool in `--dry-run` mode (see
   `scripts/kafka-dlt-replay.sh`) to log what *would* be republished without actually
   producing messages.
4. **Replay:** run the tool for real, republishing DLT messages back onto the original
   topic (not directly "processing" them out-of-band) so they go through the normal,
   idempotent consumer path — `eventId` is preserved, so if some of them were partially
   processed already, the idempotency guard prevents double-application.
5. **Monitor:** watch `consumer_processing_errors_total` and the DLT topic's volume metric
   to confirm the replayed batch is now processing cleanly.
6. **Document:** record the incident, root cause, and replay batch size in the operational
   log referenced from `docs/disaster-recovery/`.

## Guardrails

- Replay is always performed by an authorized operator, never automated as a blind
  requeue-on-failure loop (that would reintroduce the poison-message problem).
- Replay tooling requires the same IAM/RBAC access as any other production write path —
  it is not a break-glass bypass of normal access control.
