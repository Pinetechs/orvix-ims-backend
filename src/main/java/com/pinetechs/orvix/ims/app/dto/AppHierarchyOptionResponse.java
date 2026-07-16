package com.pinetechs.orvix.ims.app.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryHierarchyProgressStatus;

import java.time.LocalDateTime;

public class AppHierarchyOptionResponse {
    private final Long id;
    private final String code;
    private final String name;
    private final long scanCount;
    private final LocalDateTime lastScanAt;
    private final InventoryHierarchyProgressStatus progressStatus;
    private final boolean completionEnabled;
    private final boolean canComplete;
    private final LocalDateTime completedAt;

    public AppHierarchyOptionResponse(Long id, String code, String name) {
        this(id, code, name, 0L, null, InventoryHierarchyProgressStatus.NOT_STARTED,
                false, false, null);
    }

    public AppHierarchyOptionResponse(
            Long id,
            String code,
            String name,
            long scanCount,
            LocalDateTime lastScanAt,
            InventoryHierarchyProgressStatus progressStatus,
            boolean completionEnabled,
            boolean canComplete,
            LocalDateTime completedAt
    ) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.scanCount = Math.max(0L, scanCount);
        this.lastScanAt = lastScanAt;
        this.progressStatus = progressStatus == null
                ? InventoryHierarchyProgressStatus.NOT_STARTED
                : progressStatus;
        this.completionEnabled = completionEnabled;
        this.canComplete = canComplete;
        this.completedAt = completedAt;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public long getScanCount() { return scanCount; }
    public LocalDateTime getLastScanAt() { return lastScanAt; }
    public InventoryHierarchyProgressStatus getProgressStatus() { return progressStatus; }
    public boolean isCompletionEnabled() { return completionEnabled; }
    public boolean isCanComplete() { return canComplete; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
