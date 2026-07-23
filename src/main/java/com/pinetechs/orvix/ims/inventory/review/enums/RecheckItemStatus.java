package com.pinetechs.orvix.ims.inventory.review.enums;

public enum RecheckItemStatus {
    PENDING,
    SUBMITTED,
    ACCEPTED,
    REJECTED,
    CANCELLED;

    public boolean isFinal() {
        return this == ACCEPTED || this == REJECTED || this == CANCELLED;
    }
}
