package com.recnaile.indent.domain.state;

import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.exception.InvalidIndentTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class IndentStateMachine {

    private static final Map<IndentStatus, Set<IndentStatus>> ALLOWED =
            new EnumMap<>(IndentStatus.class);

    static {
        ALLOWED.put(IndentStatus.DRAFT,
                EnumSet.of(IndentStatus.SUBMITTED));
        ALLOWED.put(IndentStatus.SUBMITTED,
                EnumSet.of(IndentStatus.UNDER_VERIFICATION));
        ALLOWED.put(IndentStatus.UNDER_VERIFICATION,
                EnumSet.of(IndentStatus.VERIFIED, IndentStatus.REJECTED));
        ALLOWED.put(IndentStatus.VERIFIED,
                EnumSet.of(IndentStatus.PENDING_APPROVAL));
        ALLOWED.put(IndentStatus.PENDING_APPROVAL,
                EnumSet.of(IndentStatus.APPROVED, IndentStatus.REJECTED));
        ALLOWED.put(IndentStatus.APPROVED,
                EnumSet.of(IndentStatus.READY_FOR_PO));
        ALLOWED.put(IndentStatus.READY_FOR_PO,
                EnumSet.noneOf(IndentStatus.class));
        ALLOWED.put(IndentStatus.REJECTED,
                EnumSet.noneOf(IndentStatus.class));
    }

    private IndentStateMachine() {
    }

    public static boolean canTransition(IndentStatus from, IndentStatus to) {
        return from != null
                && to != null
                && ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void validateTransition(IndentStatus from, IndentStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidIndentTransitionException(from, to);
        }
    }
}
