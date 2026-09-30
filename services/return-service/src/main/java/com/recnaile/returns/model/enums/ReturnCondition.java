package com.recnaile.returns.model.enums;

/**
 * Physical condition recorded during return verification.
 *
 * The values are aligned with the documented Return Service workflow:
 * GOOD returns restore available stock and DAMAGED returns increase defect stock.
 */
public enum ReturnCondition {
    GOOD,
    DAMAGED
}
