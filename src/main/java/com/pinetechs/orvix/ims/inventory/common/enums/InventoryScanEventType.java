package com.pinetechs.orvix.ims.inventory.common.enums;

/**
 * Describes why a scan event was created. The business result is stored in the
 * domain-specific scan result enum; keeping the event type separate avoids
 * losing audit information when an item is scanned more than once.
 */
public enum InventoryScanEventType {
    FIRST_SCAN,
    DUPLICATE,
    CONFLICT,
    EXTRA,
    AMBIGUOUS,
    CORRECTION,
    RECHECK
}
