package com.pinetechs.orvix.ims.inventory.tracking.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaLevel;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaStatus;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAttentionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class TrackingResponses {

    private TrackingResponses() {}

    public record UserRef(Long id, String name, String username, String mobile) {}

    public record CompanyRef(Long id, String code, String name) {}

    public record CurrentMetrics(
            long totalExpected,
            long processedExpected,
            long matched,
            long mismatched,
            long remaining,
            long acceptedWithoutImage,
            long acceptedWithNotes,
            int progressPercentage,
            BigDecimal totalExpectedQuantity,
            BigDecimal processedExpectedQuantity,
            BigDecimal actualQuantity,
            BigDecimal shortageQuantity,
            BigDecimal overageQuantity,
            BigDecimal netVarianceQuantity
    ) {
        public static CurrentMetrics empty() {
            return new CurrentMetrics(0, 0, 0, 0, 0, 0, 0, 0,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    public record EventMetrics(
            long totalEvents,
            long firstScans,
            long duplicates,
            long conflicts,
            long corrections,
            long extraEvents,
            long ambiguousEvents,
            long uniqueUnexpectedCodes,
            LocalDateTime firstActivityAt,
            LocalDateTime lastActivityAt
    ) {
        public static EventMetrics empty() {
            return new EventMetrics(0, 0, 0, 0, 0, 0, 0, 0, null, null);
        }
    }

    public record AllowedActions(
            boolean editScanSettings,
            boolean editAssignments,
            boolean markReady,
            boolean start,
            boolean pause,
            boolean resume,
            boolean submitForReview,
            boolean returnToProgress,
            boolean complete,
            boolean cancel
    ) {}

    public record TaskListItem(
            Long taskId,
            String taskNumber,
            String taskName,
            CompanyRef company,
            InventoryDomain domain,
            InventoryTaskStatus status,
            CurrentMetrics execution,
            EventMetrics activity,
            long attentionCount,
            boolean stalled,
            LocalDateTime startedAt,
            LocalDateTime reviewStartedAt,
            LocalDateTime closedAt,
            LocalDateTime updatedAt
    ) {}

    public record TaskOverview(
            Long taskId,
            String taskNumber,
            String taskName,
            String description,
            CompanyRef company,
            InventoryDomain domain,
            InventoryTaskStatus status,
            boolean scanImageRequired,
            CurrentMetrics execution,
            EventMetrics activity,
            long activeWorkingSeconds,
            long attentionCount,
            boolean stalled,
            LocalDateTime startedAt,
            LocalDateTime pausedAt,
            String pauseReason,
            LocalDateTime reviewStartedAt,
            LocalDateTime closedAt,
            LocalDateTime generatedAt,
            AllowedActions allowedActions
    ) {}

    public record Area(
            String key,
            String parentKey,
            Long id,
            Long rootAreaId,
            TrackingAreaLevel level,
            String code,
            String name,
            long planned,
            long processed,
            long matched,
            long mismatched,
            long remaining,
            long extrasFoundHere,
            long scanEvents,
            int progressPercentage,
            TrackingAreaStatus status,
            LocalDateTime lastActivityAt,
            List<UserRef> assignedStaff
    ) {}

    public record TeamMember(
            UserRef user,
            boolean currentlyAssigned,
            List<String> assignedAreas,
            long acceptedExpectedItems,
            long matched,
            long mismatched,
            long scanEvents,
            long firstScans,
            long duplicates,
            long conflicts,
            long corrections,
            long extras,
            LocalDateTime firstActivityAt,
            LocalDateTime lastActivityAt,
            Long inactiveSeconds
    ) {}

    public record ResultItem(
            Long itemId,
            InventoryDomain domain,
            String code,
            String secondaryCode,
            String description,
            String status,
            boolean processed,
            boolean matched,
            String expectedArea,
            String expectedSubArea,
            String expectedLeafArea,
            String actualArea,
            String actualSubArea,
            String actualLeafArea,
            BigDecimal expectedQuantity,
            BigDecimal actualQuantity,
            BigDecimal varianceQuantity,
            UserRef acceptedBy,
            LocalDateTime acceptedAt,
            Long currentScanId,
            boolean hasImage,
            String imageUrl,
            String notes
    ) {}

    public record ScanEvent(
            Long scanId,
            InventoryDomain domain,
            String scannedCode,
            InventoryScanEventType eventType,
            String result,
            Long itemId,
            String expectedArea,
            String expectedSubArea,
            String expectedLeafArea,
            String actualArea,
            String actualSubArea,
            String actualLeafArea,
            BigDecimal expectedQuantity,
            BigDecimal actualQuantity,
            BigDecimal varianceQuantity,
            UserRef scannedBy,
            LocalDateTime scannedAt,
            LocalDateTime deviceScannedAt,
            String deviceId,
            String symbology,
            boolean hasImage,
            String imageUrl,
            String notes,
            String details
    ) {}

    public record AttentionSummary(
            long totalIssues,
            long currentMismatches,
            long missingRequiredImages,
            long extraEvents,
            long uniqueUnexpectedCodes,
            long ambiguousEvents,
            long conflicts,
            long staffNotes,
            long stalledAreas,
            long notStartedAreas,
            boolean readyForReview,
            boolean importFailed,
            boolean highDuplicateRate
    ) {}

    public record AttentionItem(
            String key,
            TrackingAttentionType type,
            String title,
            String description,
            Long itemId,
            Long scanId,
            String code,
            String expectedArea,
            String actualArea,
            UserRef relatedUser,
            LocalDateTime occurredAt,
            boolean hasImage,
            String imageUrl
    ) {}

    public record Attention(
            AttentionSummary summary,
            List<AttentionItem> items,
            boolean truncated,
            LocalDateTime generatedAt
    ) {}

    public record ImageFile(
            Long fileId,
            String filePath,
            String originalFileName,
            String storedFileName,
            String contentType,
            Long fileSize
    ) {}
}
