package com.recnaile.inward.model;

/**
 * Lifecycle states used by the GRN aggregate.
 *
 * Phase 02 defines the domain vocabulary only.
 * Transition rules are implemented in Phase 03.
 */
public enum GRNStatus {
    DRAFT,
    RECEIVED,
    PARTIALLY_RECEIVED,
    COMPLETED
}
