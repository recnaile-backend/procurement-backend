package com.recnaile.outward.domain.state;

import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.exception.InvalidOutwardTransitionException;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OutwardStateMachine {

    private static final Map<OutwardStatus, Set<OutwardStatus>> ALLOWED = build();

    private OutwardStateMachine() {}

    public static void transition(OutwardRequest outward, OutwardStatus target, Instant at) {
        if (outward == null || target == null || at == null) {
            throw new IllegalArgumentException("outward, target and transition time are required");
        }
        OutwardStatus current = OutwardStatus.valueOf(outward.getStatus());
        if (!isAllowed(current, target)) {
            throw new InvalidOutwardTransitionException(
                    "INVALID_OUTWARD_STATE_TRANSITION",
                    "Outward cannot transition from " + current + " to " + target);
        }
        outward.setStatus(target.name());
    }

    public static boolean isAllowed(OutwardStatus from, OutwardStatus to) {
        return from != null && to != null &&
                ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    private static Map<OutwardStatus, Set<OutwardStatus>> build() {
        EnumMap<OutwardStatus, Set<OutwardStatus>> m = new EnumMap<>(OutwardStatus.class);
        m.put(OutwardStatus.PENDING_APPROVAL,
                EnumSet.of(OutwardStatus.APPROVED, OutwardStatus.REJECTED, OutwardStatus.CANCELLED));
        m.put(OutwardStatus.APPROVED, EnumSet.of(OutwardStatus.ISSUED, OutwardStatus.CANCELLED));
        m.put(OutwardStatus.ISSUED, EnumSet.noneOf(OutwardStatus.class));
        m.put(OutwardStatus.REJECTED, EnumSet.noneOf(OutwardStatus.class));
        m.put(OutwardStatus.CANCELLED, EnumSet.noneOf(OutwardStatus.class));
        return Map.copyOf(m);
    }
}
