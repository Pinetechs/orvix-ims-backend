package com.pinetechs.orvix.ims.inventory.review.enums;

public enum ReviewIssueStatus {
    OPEN,
    RECHECK_REQUESTED,
    RECHECK_IN_PROGRESS,
    RECHECK_SUBMITTED,
    RESOLVED,
    SUPERSEDED;

    public boolean isOpen() {
        return this != RESOLVED && this != SUPERSEDED;
    }
}
