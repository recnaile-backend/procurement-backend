package com.recnaile.reporting.service;

import com.recnaile.reporting.model.ReportFreshness;
import com.recnaile.reporting.model.ReportingFact;
import com.recnaile.reporting.model.ReportingProjection;
import com.recnaile.reporting.model.ReportPeriod;
import com.recnaile.reporting.repository.ReportingFactRepository;
import com.recnaile.reporting.repository.ReportingProjectionRepository;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Map;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportingDashboardQueryTest {
    @Mock ReportingProjectionRepository projectionRepository;
    @Mock ReportingFactRepository factRepository;
    @Mock MongoTemplate mongo;
    @Mock AggregationResults<Document> aggregationResults;

    private ReportingQueryService service;

    @BeforeEach
    void setUp() {
        service = new ReportingQueryService(projectionRepository, factRepository, mongo);
    }

    private void stubDashboardAggregations() {
        when(mongo.aggregate(any(Aggregation.class), eq("reporting_projections"), eq(Document.class)))
            .thenReturn(aggregationResults);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(null);
    }

    @Test
    void dashboardReturnsCompleteKpiSetAndScopedFreshness() {
        stubDashboardAggregations();
        Map<String, Object> result = service.dashboard("DEPT-7", "PROJECT-9");

        assertEquals(0L, result.get("pendingIndents"));
        assertTrue(result.containsKey("approvalAgingOver24Hours"));
        assertTrue(result.containsKey("approvalAgingOver72Hours"));
        assertTrue(result.containsKey("purchaseOrdersTotal"));
        assertTrue(result.containsKey("purchaseOrderEstimatedValue"));
        assertTrue(result.containsKey("acceptedInwardQuantity"));
        assertTrue(result.containsKey("availableQuantityAtLowStockAlerts"));
        assertTrue(result.containsKey("outwardIssuedQuantity"));
        assertTrue(result.containsKey("returnedQuantity"));
        assertTrue(result.containsKey("slaBreaches"));
        assertTrue(result.get("generatedAt") instanceof java.time.Instant);
        assertEquals(Map.of("departmentName", "DEPT-7", "projectId", "PROJECT-9"), result.get("filters"));
        assertTrue(result.get("freshness") instanceof ReportFreshness);

        ArgumentCaptor<Query> projectionQueries = ArgumentCaptor.forClass(Query.class);
        verify(mongo, atLeastOnce()).count(projectionQueries.capture(), eq(ReportingProjection.class));
        assertTrue(projectionQueries.getAllValues().stream().anyMatch(q ->
            "DEPT-7".equals(q.getQueryObject().getString("departmentName"))
                && "PROJECT-9".equals(q.getQueryObject().getString("projectId"))));
    }

    @Test
    void dashboardWithNoScopeClearlyReportsAllScope() {
        stubDashboardAggregations();
        Map<String, Object> result = service.dashboard(null, null);
        assertEquals(Map.of("departmentName", "ALL", "projectId", "ALL"), result.get("filters"));
    }
    @Test
    void periodSummaryAppliesScopeAndReturnsRequestedPeriodMetadata() {
        when(mongo.find(any(Query.class), eq(ReportingFact.class))).thenReturn(List.of());
        Map<String, Object> result = service.periodSummary(ReportPeriod.MONTHLY,
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 12), "DEPT-7", "PROJECT-9");
        assertEquals(ReportPeriod.MONTHLY, result.get("period"));
        assertEquals(LocalDate.of(2026, 10, 1), result.get("from"));
        assertEquals(LocalDate.of(2026, 10, 12), result.get("to"));
        assertEquals(Map.of("departmentName", "DEPT-7", "projectId", "PROJECT-9"), result.get("filters"));
        ArgumentCaptor<Query> factQueries = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(factQueries.capture(), eq(ReportingFact.class));
        assertEquals("DEPT-7", factQueries.getValue().getQueryObject().getString("departmentName"));
        assertEquals("PROJECT-9", factQueries.getValue().getQueryObject().getString("projectId"));
    }

    @Test
    void periodSummaryRejectsReversedDateRange() {
        assertThrows(IllegalArgumentException.class, () -> service.periodSummary(ReportPeriod.DAILY,
            java.time.LocalDate.of(2026, 10, 12), java.time.LocalDate.of(2026, 10, 11), null, null));
    }

}
