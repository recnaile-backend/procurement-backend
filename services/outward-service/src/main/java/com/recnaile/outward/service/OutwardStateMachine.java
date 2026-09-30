package com.recnaile.outward.service;

import com.recnaile.outward.domain.enums.OutwardStatus;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class OutwardStateMachine {

    private final Map<OutwardStatus, Set<OutwardStatus>> transitions;

    public OutwardStateMachine() {
        EnumMap<OutwardStatus, Set<OutwardStatus>> configured = new EnumMap<>(OutwardStatus.class);
        configured.put(OutwardStatus.DRAFT,
                EnumSet.of(OutwardStatus.PENDING_APPROVAL));
        configured.put(OutwardStatus.PENDING_APPROVAL,
                EnumSet.of(OutwardStatus.APPROVED, OutwardStatus.REJECTED));
        configured.put(OutwardStatus.APPROVED,
                EnumSet.of(OutwardStatus.ISSUED));
        configured.put(OutwardStatus.REJECTED, EnumSet.noneOf(OutwardStatus.class));
        configured.put(OutwardStatus.ISSUED, EnumSet.noneOf(OutwardStatus.class));
        configured.put(OutwardStatus.CANCELLED, EnumSet.noneOf(OutwardStatus.class));

        EnumMap<OutwardStatus, Set<OutwardStatus>> immutable = new EnumMap<>(OutwardStatus.class);
        configured.forEach((status, targets) ->
                immutable.put(status, immutableSet(targets)));
        this.transitions = Collections.unmodifiableMap(immutable);
    }

    private Set<OutwardStatus> immutableSet(Set<OutwardStatus> targets) {
        if (targets.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(targets));
    }

    public boolean canTransition(OutwardStatus currentStatus, OutwardStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        return transitions.getOrDefault(currentStatus, Collections.emptySet()).contains(targetStatus);
    }

    public OutwardStatus transition(OutwardStatus currentStatus, OutwardStatus targetStatus) {
        if (!canTransition(currentStatus, targetStatus)) {
            throw new InvalidOutwardStateTransitionException(currentStatus, targetStatus);
        }
        return targetStatus;
    }

    public Set<OutwardStatus> allowedTransitions(OutwardStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }
        return transitions.getOrDefault(currentStatus, Collections.emptySet());
    }
}
