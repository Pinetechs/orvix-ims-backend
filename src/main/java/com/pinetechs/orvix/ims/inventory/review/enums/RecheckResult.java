package com.pinetechs.orvix.ims.inventory.review.enums;

public enum RecheckResult {
    FOUND_MATCHED,
    FOUND_DIFFERENT_LOCATION,
    QUANTITY_CONFIRMED,
    QUANTITY_DIFFERENT,
    NOT_FOUND,
    UNABLE_TO_VERIFY;

    public boolean hasObservedResult() {
        return this == FOUND_MATCHED
                || this == FOUND_DIFFERENT_LOCATION
                || this == QUANTITY_CONFIRMED
                || this == QUANTITY_DIFFERENT;
    }
}
