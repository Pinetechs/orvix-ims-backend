package com.pinetechs.orvix.ims.inventory.review.enums;

public enum RecheckRequestStatus {
    PENDING,
    IN_PROGRESS,
    SUBMITTED,
    COMPLETED,
    CANCELLED;

    public boolean isActive() {
        return this == PENDING || this == IN_PROGRESS || this == SUBMITTED;
    }
}
