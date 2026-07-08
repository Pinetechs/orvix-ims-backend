package com.pinetechs.orvix.ims.inventory.sparepart.enums;

public enum SparePartInventoryScanResult {
    MATCHED,
    SHORTAGE,
    OVERAGE,
    LOCATION_MISMATCH,
    LOCATION_MISMATCH_WITH_SHORTAGE,
    LOCATION_MISMATCH_WITH_OVERAGE,
    EXTRA,
    RECOUNT_SAME_LOCATION,
    RECOUNT_DIFFERENT_LOCATION,
    NOT_ASSIGNED_BRANCH
}
