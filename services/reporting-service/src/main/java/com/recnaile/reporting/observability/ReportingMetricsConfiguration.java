package com.recnaile.reporting.observability;

import com.recnaile.reporting.repository.ReportingFactRepository;
import com.recnaile.reporting.repository.ReportingProjectionRepository;
import com.recnaile.reporting.repository.ReportingSnapshotRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

@Configuration
public class ReportingMetricsConfiguration {
    @Bean
    public ReportingCollectionMetrics reportingCollectionMetrics(ReportingFactRepository facts,
            ReportingProjectionRepository projections, ReportingSnapshotRepository snapshots,
            MeterRegistry registry) {
        Gauge.builder("reporting.facts.count", facts, repository -> repository.count())
                .description("Number of retained immutable reporting event facts").register(registry);
        Gauge.builder("reporting.projections.count", projections, repository -> repository.count())
                .description("Number of current reporting projections").register(registry);
        Gauge.builder("reporting.snapshots.count", snapshots, repository -> repository.count())
                .description("Number of retained historical reporting snapshots").register(registry);
        return new ReportingCollectionMetrics();
    }

    /** Marker bean documents that the collection gauges are installed. */
    public static final class ReportingCollectionMetrics { }
}
