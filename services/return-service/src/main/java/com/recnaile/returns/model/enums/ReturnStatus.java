package com.recnaile.returns.model.enums;

/**
 * Lifecycle status of a return request.
 *
 * Transition rules are implemented in the Return Service application layer in
 * the dedicated state-machine phase. This enum only defines the persisted states.
 */
public enum ReturnStatus {
    PENDING_VERIFICATION,
    VERIFIED
}
