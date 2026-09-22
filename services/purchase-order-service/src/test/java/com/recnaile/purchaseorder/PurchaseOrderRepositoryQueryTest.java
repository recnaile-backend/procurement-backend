package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.repository.Query;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseOrderRepositoryQueryTest {

    @Test
    void currentQueriesExcludeLegacyClosedDocuments() throws Exception {
        for (String methodName : new String[] {
                "findAllCurrent",
                "findCurrentByVendorName",
                "findCurrentByProjectId",
                "findCurrentByDepartmentName",
                "findCurrentByVendorNameAndProjectId",
                "findCurrentByVendorNameAndDepartmentName",
                "findCurrentByProjectIdAndDepartmentName",
                "findCurrentByVendorNameAndProjectIdAndDepartmentName",
                "findCurrentByPoId",
                "findCurrentBySourceIndentIdsIn"
        }) {
            Method method = java.util.Arrays.stream(PurchaseOrderRepository.class.getMethods())
                    .filter(m -> m.getName().equals(methodName))
                    .findFirst()
                    .orElseThrow();
            Query query = method.getAnnotation(Query.class);
            assertThat(query).isNotNull();
            assertThat(query.value()).contains("'status': { '$ne': 'CLOSED' }");
        }
    }
}
