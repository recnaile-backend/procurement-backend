package com.recnaile.inward.service;

import com.recnaile.inward.exception.InvalidGRNStateTransitionException;
import com.recnaile.inward.model.GRNStatus;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class GRNStateMachine {

    private final Map<GRNStatus, Set<GRNStatus>> allowedTransitions =
            new EnumMap<>(GRNStatus.class);

    public GRNStateMachine() {
        allowedTransitions.put(
                GRNStatus.DRAFT,
                EnumSet.of(GRNStatus.RECEIVED)
        );

        allowedTransitions.put(
                GRNStatus.RECEIVED,
                EnumSet.of(GRNStatus.PARTIALLY_RECEIVED, GRNStatus.COMPLETED)
        );

        allowedTransitions.put(
                GRNStatus.PARTIALLY_RECEIVED,
                EnumSet.of(GRNStatus.COMPLETED)
        );

        allowedTransitions.put(
                GRNStatus.COMPLETED,
                EnumSet.noneOf(GRNStatus.class)
        );
    }

    public boolean canTransition(GRNStatus currentStatus, GRNStatus requestedStatus) {
        if (currentStatus == null || requestedStatus == null) {
            return false;
        }

        return allowedTransitions
                .getOrDefault(currentStatus, Set.of())
                .contains(requestedStatus);
    }

    public void validateTransition(
            GRNStatus currentStatus,
            GRNStatus requestedStatus
    ) {
        if (!canTransition(currentStatus, requestedStatus)) {
            throw new InvalidGRNStateTransitionException(
                    currentStatus,
                    requestedStatus
            );
        }
    }

    public Set<GRNStatus> allowedNextStates(GRNStatus currentStatus) {
        return Set.copyOf(
                allowedTransitions.getOrDefault(currentStatus, Set.of())
        );
    }
}
