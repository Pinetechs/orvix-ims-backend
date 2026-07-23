package com.pinetechs.orvix.ims.inventory.review.enums;

public enum ReviewDecisionType {
    ACCEPT_RECHECK_RESULT,
    KEEP_CURRENT_RESULT,
    CONFIRM_MISSING,
    CONFIRM_DESTROYED,
    CONFIRM_EXTRA,
    REQUEST_ANOTHER_RECHECK;

    public boolean resolvesIssue() {
        return this != REQUEST_ANOTHER_RECHECK;
    }
}
