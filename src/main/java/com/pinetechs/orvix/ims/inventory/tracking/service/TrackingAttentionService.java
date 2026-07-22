package com.pinetechs.orvix.ims.inventory.tracking.service;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingSnapshot;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaStatus;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAttentionType;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingResultFilter;
import com.pinetechs.orvix.ims.inventory.tracking.policy.TrackingStatusPolicy;
import com.pinetechs.orvix.ims.inventory.tracking.provider.InventoryTrackingProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrackingAttentionService {

    private static final int MAX_ATTENTION_ITEMS = 50;

    private final TrackingStatusPolicy statusPolicy;

    public TrackingAttentionService(TrackingStatusPolicy statusPolicy) {
        this.statusPolicy = statusPolicy;
    }

    public TrackingResponses.Attention build(InventoryTask task, InventoryTrackingProvider provider, TrackingSnapshot snapshot) {
        List<TrackingResponses.Area> areas = provider.areas(task.getId(), task.getStatus());

        AttentionFacts facts = calculateFacts(task, snapshot, areas);
        List<TrackingResponses.AttentionItem> items = loadAttentionItems(
                task,
                provider,
                snapshot,
                areas,
                facts
        );

        return new TrackingResponses.Attention(
                facts.toSummary(snapshot),
                List.copyOf(items),
                facts.totalIssues() > items.size(),
                LocalDateTime.now()
        );
    }

    private AttentionFacts calculateFacts(
            InventoryTask task,
            TrackingSnapshot snapshot,
            List<TrackingResponses.Area> areas
    ) {
        long stalledAreas = areas.stream()
                .filter(area -> area.status() == TrackingAreaStatus.STALLED)
                .filter(area -> statusPolicy.isLeafArea(task.getInventoryDomain(), area))
                .count();

        boolean runningLongEnough = statusPolicy.hasBeenRunningLongEnough(task);
        long notStartedAreas = runningLongEnough
                ? areas.stream()
                        .filter(area -> area.status() == TrackingAreaStatus.NOT_STARTED)
                        .filter(area -> statusPolicy.isLeafArea(task.getInventoryDomain(), area))
                        .count()
                : 0;

        long missingImages = task.isScanImageRequired()
                ? snapshot.current().acceptedWithoutImage()
                : 0;
        boolean readyForReview = statusPolicy.isReadyForReview(task, snapshot.current());
        boolean importFailed = task.getStatus() == InventoryTaskStatus.IMPORT_FAILED;
        boolean highDuplicateRate = statusPolicy.isHighDuplicateRate(snapshot.events());

        long totalIssues = snapshot.current().mismatched()
                + missingImages
                + snapshot.events().extraEvents()
                + snapshot.events().conflicts()
                + snapshot.current().acceptedWithNotes()
                + stalledAreas
                + notStartedAreas
                + asCount(readyForReview)
                + asCount(importFailed)
                + asCount(highDuplicateRate);

        return new AttentionFacts(
                totalIssues,
                missingImages,
                stalledAreas,
                notStartedAreas,
                runningLongEnough,
                readyForReview,
                importFailed,
                highDuplicateRate
        );
    }

    private List<TrackingResponses.AttentionItem> loadAttentionItems(InventoryTask task, InventoryTrackingProvider provider, TrackingSnapshot snapshot, List<TrackingResponses.Area> areas, AttentionFacts facts) {
        List<TrackingResponses.AttentionItem> items = new ArrayList<>();

        appendResultItems(items, provider.results(
                        task.getId(),
                        TrackingResultFilter.MISMATCHED,
                        null,
                        PageRequest.of(0, 15)
                ).getContent(),
                TrackingAttentionType.MISMATCH
        );

        if (task.isScanImageRequired()) {
            appendResultItems(
                    items,
                    provider.results(
                            task.getId(),
                            TrackingResultFilter.MISSING_IMAGE,
                            null,
                            PageRequest.of(0, 10)
                    ).getContent(),
                    TrackingAttentionType.MISSING_IMAGE
            );
        }

        appendEventItems(
                items,
                provider.scanEvents(
                        task.getId(),
                        InventoryScanEventType.EXTRA,
                        null,
                        PageRequest.of(0, 10)
                ).getContent(),
                TrackingAttentionType.EXTRA
        );
        appendEventItems(
                items,
                provider.scanEvents(
                        task.getId(),
                        InventoryScanEventType.CONFLICT,
                        null,
                        PageRequest.of(0, 10)
                ).getContent(),
                TrackingAttentionType.CONFLICT
        );
        appendResultItems(
                items,
                provider.results(
                        task.getId(),
                        TrackingResultFilter.NOTES,
                        null,
                        PageRequest.of(0, 10)
                ).getContent(),
                TrackingAttentionType.STAFF_NOTE
        );

        appendAreaItems(items, task, areas, facts.runningLongEnough());
        appendTaskItems(items, task, snapshot, facts);
        return items;
    }

    private void appendAreaItems(List<TrackingResponses.AttentionItem> target, InventoryTask task, List<TrackingResponses.Area> areas, boolean runningLongEnough) {
        for (TrackingResponses.Area area : areas) {
            if (!hasCapacity(target)) {
                return;
            }
            if (area.status() != TrackingAreaStatus.STALLED
                    || !statusPolicy.isLeafArea(task.getInventoryDomain(), area)) {
                continue;
            }
            target.add(new TrackingResponses.AttentionItem(
                    "STALLED_AREA:" + area.key(),
                    TrackingAttentionType.STALLED_AREA,
                    "Area activity has stopped",
                    area.name(),
                    null,
                    null,
                    area.code(),
                    area.name(),
                    null,
                    null,
                    area.lastActivityAt(),
                    false,
                    null
            ));
        }

        if (!runningLongEnough) {
            return;
        }
        for (TrackingResponses.Area area : areas) {
            if (!hasCapacity(target)) {
                return;
            }
            if (area.status() != TrackingAreaStatus.NOT_STARTED
                    || !statusPolicy.isLeafArea(task.getInventoryDomain(), area)) {
                continue;
            }
            target.add(new TrackingResponses.AttentionItem(
                    "AREA_NOT_STARTED:" + area.key(),
                    TrackingAttentionType.AREA_NOT_STARTED,
                    "Area has not started",
                    area.name(),
                    null,
                    null,
                    area.code(),
                    area.name(),
                    null,
                    null,
                    task.getStartedAt(),
                    false,
                    null
            ));
        }
    }

    private void appendTaskItems(
            List<TrackingResponses.AttentionItem> target,
            InventoryTask task,
            TrackingSnapshot snapshot,
            AttentionFacts facts
    ) {
        if (facts.readyForReview() && hasCapacity(target)) {
            target.add(new TrackingResponses.AttentionItem(
                    "READY_FOR_REVIEW:" + task.getId(),
                    TrackingAttentionType.READY_FOR_REVIEW,
                    "All expected items have been processed",
                    "The task is still IN_PROGRESS and can be submitted for review",
                    null,
                    null,
                    task.getTaskNumber(),
                    null,
                    null,
                    null,
                    snapshot.events().lastActivityAt(),
                    false,
                    null
            ));
        }

        if (facts.importFailed() && hasCapacity(target)) {
            target.add(new TrackingResponses.AttentionItem(
                    "IMPORT_FAILED:" + task.getId(),
                    TrackingAttentionType.IMPORT_FAILED,
                    "Inventory import failed",
                    "Review the import activity in the task timeline before retrying",
                    null,
                    null,
                    task.getTaskNumber(),
                    null,
                    null,
                    null,
                    task.getUpdatedAt(),
                    false,
                    null
            ));
        }

        if (facts.highDuplicateRate() && hasCapacity(target)) {
            target.add(new TrackingResponses.AttentionItem(
                    "HIGH_DUPLICATE_RATE:" + task.getId(),
                    TrackingAttentionType.HIGH_DUPLICATE_RATE,
                    "Duplicate scan rate is high",
                    statusPolicy.duplicateRate(snapshot.events())
                            + "% of the recorded scan events are duplicates",
                    null,
                    null,
                    task.getTaskNumber(),
                    null,
                    null,
                    null,
                    snapshot.events().lastActivityAt(),
                    false,
                    null
            ));
        }
    }

    private void appendResultItems(
            List<TrackingResponses.AttentionItem> target,
            List<TrackingResponses.ResultItem> results,
            TrackingAttentionType type
    ) {
        for (TrackingResponses.ResultItem result : results) {
            if (!hasCapacity(target)) {
                return;
            }
            target.add(new TrackingResponses.AttentionItem(
                    type + ":ITEM:" + result.itemId(),
                    type,
                    resultTitle(type),
                    type == TrackingAttentionType.STAFF_NOTE
                            ? result.notes()
                            : result.status(),
                    result.itemId(),
                    result.currentScanId(),
                    result.code(),
                    joinArea(result.expectedArea(), result.expectedSubArea(), result.expectedLeafArea()),
                    joinArea(result.actualArea(), result.actualSubArea(), result.actualLeafArea()),
                    result.acceptedBy(),
                    result.acceptedAt(),
                    result.hasImage(),
                    result.imageUrl()
            ));
        }
    }

    private void appendEventItems(
            List<TrackingResponses.AttentionItem> target,
            List<TrackingResponses.ScanEvent> events,
            TrackingAttentionType type
    ) {
        for (TrackingResponses.ScanEvent event : events) {
            if (!hasCapacity(target)) {
                return;
            }
            target.add(new TrackingResponses.AttentionItem(
                    type + ":SCAN:" + event.scanId(),
                    type,
                    type == TrackingAttentionType.EXTRA
                            ? "Unexpected code was scanned"
                            : "The code was scanned in a conflicting location",
                    event.details(),
                    event.itemId(),
                    event.scanId(),
                    event.scannedCode(),
                    joinArea(event.expectedArea(), event.expectedSubArea(), event.expectedLeafArea()),
                    joinArea(event.actualArea(), event.actualSubArea(), event.actualLeafArea()),
                    event.scannedBy(),
                    event.scannedAt(),
                    event.hasImage(),
                    event.imageUrl()
            ));
        }
    }

    private String resultTitle(TrackingAttentionType type) {
        return switch (type) {
            case MISMATCH -> "Current result does not match the expected record";
            case MISSING_IMAGE -> "Required scan image is missing";
            case STAFF_NOTE -> "Inventory staff added a note";
            default -> type.name();
        };
    }

    private String joinArea(String first, String second, String third) {
        return Arrays.asList(first, second, third).stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" / "));
    }

    private boolean hasCapacity(List<?> items) {
        return items.size() < MAX_ATTENTION_ITEMS;
    }

    private long asCount(boolean value) {
        return value ? 1 : 0;
    }

    private record AttentionFacts(
            long totalIssues,
            long missingImages,
            long stalledAreas,
            long notStartedAreas,
            boolean runningLongEnough,
            boolean readyForReview,
            boolean importFailed,
            boolean highDuplicateRate
    ) {
        private TrackingResponses.AttentionSummary toSummary(TrackingSnapshot snapshot) {
            return new TrackingResponses.AttentionSummary(
                    totalIssues,
                    snapshot.current().mismatched(),
                    missingImages,
                    snapshot.events().extraEvents(),
                    snapshot.events().uniqueUnexpectedCodes(),
                    snapshot.events().conflicts(),
                    snapshot.current().acceptedWithNotes(),
                    stalledAreas,
                    notStartedAreas,
                    readyForReview,
                    importFailed,
                    highDuplicateRate
            );
        }
    }
}
