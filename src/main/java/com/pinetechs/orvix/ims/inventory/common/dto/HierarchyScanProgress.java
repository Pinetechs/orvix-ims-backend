package com.pinetechs.orvix.ims.inventory.common.dto;

import java.time.LocalDateTime;

/** Aggregated scan activity for one location, floor or place. */
public class HierarchyScanProgress {

    private final Long hierarchyId;
    private final Long scanCount;
    private final LocalDateTime lastScanAt;
    private final Long reviewRequiredCount;

    public HierarchyScanProgress(Long hierarchyId, Long scanCount, LocalDateTime lastScanAt) {
        this(hierarchyId, scanCount, lastScanAt, 0L);
    }

    public HierarchyScanProgress(
            Long hierarchyId,
            Long scanCount,
            LocalDateTime lastScanAt,
            Long reviewRequiredCount
    ) {
        this.hierarchyId = hierarchyId;
        this.scanCount = scanCount == null ? 0L : scanCount;
        this.lastScanAt = lastScanAt;
        this.reviewRequiredCount = reviewRequiredCount == null ? 0L : reviewRequiredCount;
    }

    public Long getHierarchyId() { return hierarchyId; }
    public Long getScanCount() { return scanCount; }
    public LocalDateTime getLastScanAt() { return lastScanAt; }
    public Long getReviewRequiredCount() { return reviewRequiredCount; }
}
