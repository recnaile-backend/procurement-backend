package com.recnaile.reporting.model;

import java.time.Instant;

public record ReportFreshness(Instant lastEventOccurredAt, Instant lastUpdatedAt, long eventCount, boolean eventuallyConsistent) {}
