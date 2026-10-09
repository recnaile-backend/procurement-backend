package com.recnaile.asset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import com.recnaile.asset.api.dto.AssetGroupResponse;
import com.recnaile.asset.event.AssetAuditEventPublisher;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;
import com.recnaile.asset.repository.AssetRepository;
import org.junit.jupiter.api.Test;

class AssetGroupServiceTest {

    private final AssetRepository repository = mock(AssetRepository.class);
    private final AssetIdGenerator idGenerator = mock(AssetIdGenerator.class);
    private final AssetAuditEventPublisher auditPublisher = mock(AssetAuditEventPublisher.class);
    private final AssetService service = new AssetService(repository, idGenerator, auditPublisher);

    @Test
    void groupsPhysicalAssetsByOutwardAndItemAndCalculatesLifecycleCounts() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Asset one = Asset.create(new AssetId("ASSET%2F26-27%2F001"), "Item-3", "OUT%2F26-27%2F008",
                "Item-3", null, null, "issuer", AssetStatus.IN_USE, AssetCondition.GOOD, null, now);
        one.assignTo("EMP2", "Software", now);
        Asset two = Asset.create(new AssetId("ASSET%2F26-27%2F002"), "Item-3", "OUT%2F26-27%2F008",
                "Item-3", null, null, "issuer", AssetStatus.RETURNED, AssetCondition.GOOD, null, now);
        Asset three = Asset.create(new AssetId("ASSET%2F26-27%2F003"), "Item-3", "OUT%2F26-27%2F008",
                "Item-3", null, null, "issuer", AssetStatus.UNDER_REPAIR, AssetCondition.UNDER_REPAIR, null, now);
        Asset four = Asset.create(new AssetId("ASSET%2F26-27%2F004"), "Item-3", "OUT%2F26-27%2F008",
                "Item-3", null, null, "issuer", AssetStatus.IN_USE, AssetCondition.GOOD, null, now);
        four.assignTo("EMP3", "Software", now);

        List<AssetGroupResponse> groups = service.toGroups(List.of(one, two, three, four));

        assertEquals(1, groups.size());
        AssetGroupResponse group = groups.get(0);
        assertEquals("OUT%2F26-27%2F008", group.outwardNumber());
        assertEquals("Item-3", group.itemId());
        assertEquals(4, group.totalAssets());
        assertEquals(2, group.inUse());
        assertEquals(1, group.returned());
        assertEquals(1, group.underRepair());
        assertEquals(0, group.retired());
        assertEquals(List.of("EMP2", "EMP3"), group.assignedUserIds());
    }

    @Test
    void emptyAssetsProduceNoGroups() {
        assertEquals(List.of(), service.toGroups(List.of()));
    }
}
