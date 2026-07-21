package com.pinetechs.orvix.ims.inventory.tracking.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaStatus;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAttentionType;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingResultFilter;
import com.pinetechs.orvix.ims.inventory.tracking.provider.InventoryTrackingProvider;
import com.pinetechs.orvix.ims.inventory.tracking.provider.InventoryTrackingProviderRegistry;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryTrackingService {

    private static final long STALLED_AFTER_MINUTES = 30;
    private static final int MAX_ATTENTION_ITEMS = 50;

    private final TrackingTaskScopeService taskScopeService;
    private final InventoryTrackingProviderRegistry providerRegistry;
    private final TrackingDurationService durationService;

    public InventoryTrackingService(
            TrackingTaskScopeService taskScopeService,
            InventoryTrackingProviderRegistry providerRegistry,
            TrackingDurationService durationService
    ) {
        this.taskScopeService = taskScopeService;
        this.providerRegistry = providerRegistry;
        this.durationService = durationService;
    }

    @Transactional(readOnly = true)
    public Page<TrackingResponses.TaskListItem> findTasks(
            String search,
            Long companyId,
            InventoryDomain domain,
            InventoryTaskStatus status,
            Pageable pageable,
            User currentUser
    ) {
        Page<InventoryTask> tasks = taskScopeService.findAccessibleTasks(
                currentUser, search, companyId, domain, status, pageable);
        Map<Long, TaskSnapshot> snapshots = snapshots(tasks.getContent());
        List<TrackingResponses.TaskListItem> content = tasks.getContent().stream()
                .map(task -> toListItem(task, snapshots.get(task.getId())))
                .toList();
        return new PageImpl<>(content, pageable, tasks.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TrackingResponses.TaskOverview overview(Long taskId, User currentUser) {
        InventoryTask task = taskScopeService.requireAccessibleTask(taskId, currentUser);
        TaskSnapshot snapshot = snapshots(List.of(task)).get(taskId);
        return new TrackingResponses.TaskOverview(
                task.getId(),
                task.getTaskNumber(),
                task.getTaskName(),
                task.getDescription(),
                companyRef(task),
                task.getInventoryDomain(),
                task.getStatus(),
                task.isScanImageRequired(),
                snapshot.current(),
                snapshot.events(),
                durationService.activeWorkingSeconds(task),
                attentionCount(task, snapshot),
                isStalled(task, snapshot.events()),
                task.getStartedAt(),
                task.getPausedAt(),
                task.getPauseReason(),
                task.getReviewStartedAt(),
                task.getClosedAt(),
                LocalDateTime.now(),
                allowedActions(task, currentUser)
        );
    }

    @Transactional(readOnly = true)
    public List<TrackingResponses.Area> areas(Long taskId, User currentUser) {
        InventoryTask task = taskScopeService.requireAccessibleTask(taskId, currentUser);
        return provider(task).areas(taskId, task.getStatus());
    }

    @Transactional(readOnly = true)
    public List<TrackingResponses.TeamMember> team(Long taskId, User currentUser) {
        InventoryTask task = taskScopeService.requireAccessibleTask(taskId, currentUser);
        return provider(task).team(taskId);
    }

    @Transactional(readOnly = true)
    public Page<TrackingResponses.ResultItem> results(
            Long taskId,
            TrackingResultFilter filter,
            String search,
            Pageable pageable,
            User currentUser
    ) {
        InventoryTask task = taskScopeService.requireAccessibleTask(taskId, currentUser);
        TrackingResultFilter resolved = filter == null ? TrackingResultFilter.ALL : filter;
        if (resolved == TrackingResultFilter.MISSING_IMAGE && !task.isScanImageRequired()) {
            return Page.empty(pageable);
        }
        Page<TrackingResponses.ResultItem> results = provider(task)
                .results(taskId, resolved, search, pageable);
        if ((task.getStatus() == InventoryTaskStatus.UNDER_REVIEW
                || task.getStatus() == InventoryTaskStatus.COMPLETED)
                && task.getInventoryDomain() != InventoryDomain.SPARE_PART) {
            return results.map(this::markRemainingAsNotFound);
        }
        return results;
    }

    @Transactional(readOnly = true)
    public Page<TrackingResponses.ScanEvent> scanEvents(
            Long taskId,
            InventoryScanEventType eventType,
            String search,
            Pageable pageable,
            User currentUser
    ) {
        InventoryTask task = taskScopeService.requireAccessibleTask(taskId, currentUser);
        return provider(task).scanEvents(taskId, eventType, search, pageable);
    }

    @Transactional(readOnly = true)
    public TrackingResponses.Attention attention(Long taskId, User currentUser) {
        InventoryTask task = taskScopeService.requireAccessibleTask(taskId, currentUser);
        InventoryTrackingProvider provider = provider(task);
        TaskSnapshot snapshot = snapshots(List.of(task)).get(taskId);
        List<TrackingResponses.Area> areas = provider.areas(taskId, task.getStatus());
        long stalledAreas = areas.stream().filter(area -> area.status() == TrackingAreaStatus.STALLED
                && isLeafArea(task.getInventoryDomain(), area)).count();
        boolean taskHasBeenRunningLongEnough = task.getStatus() == InventoryTaskStatus.IN_PROGRESS
                && task.getStartedAt() != null
                && task.getStartedAt().isBefore(LocalDateTime.now().minusMinutes(STALLED_AFTER_MINUTES));
        long notStartedAreas = taskHasBeenRunningLongEnough
                ? areas.stream().filter(area -> area.status() == TrackingAreaStatus.NOT_STARTED
                        && isLeafArea(task.getInventoryDomain(), area)).count()
                : 0;
        long missingImages = task.isScanImageRequired() ? snapshot.current().acceptedWithoutImage() : 0;
        boolean readyForReview = task.getStatus() == InventoryTaskStatus.IN_PROGRESS
                && snapshot.current().totalExpected() > 0
                && snapshot.current().remaining() == 0;
        boolean importFailed = task.getStatus() == InventoryTaskStatus.IMPORT_FAILED;
        boolean highDuplicateRate = isHighDuplicateRate(snapshot.events());

        long totalIssues = snapshot.current().mismatched()
                + missingImages
                + snapshot.events().extraEvents()
                + snapshot.events().conflicts()
                + snapshot.current().acceptedWithNotes()
                + stalledAreas
                + notStartedAreas
                + (readyForReview ? 1 : 0)
                + (importFailed ? 1 : 0)
                + (highDuplicateRate ? 1 : 0);

        TrackingResponses.AttentionSummary summary = new TrackingResponses.AttentionSummary(
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

        List<TrackingResponses.AttentionItem> items = new ArrayList<>();
        appendResultAttention(items, provider.results(taskId, TrackingResultFilter.MISMATCHED,
                null, PageRequest.of(0, 15)).getContent(), TrackingAttentionType.MISMATCH);
        if (task.isScanImageRequired()) {
            appendResultAttention(items, provider.results(taskId, TrackingResultFilter.MISSING_IMAGE,
                    null, PageRequest.of(0, 10)).getContent(), TrackingAttentionType.MISSING_IMAGE);
        }
        appendEventAttention(items, provider.scanEvents(taskId, InventoryScanEventType.EXTRA,
                null, PageRequest.of(0, 10)).getContent(), TrackingAttentionType.EXTRA);
        appendEventAttention(items, provider.scanEvents(taskId, InventoryScanEventType.CONFLICT,
                null, PageRequest.of(0, 10)).getContent(), TrackingAttentionType.CONFLICT);
        appendResultAttention(items, provider.results(taskId, TrackingResultFilter.NOTES,
                null, PageRequest.of(0, 10)).getContent(), TrackingAttentionType.STAFF_NOTE);

        for (TrackingResponses.Area area : areas) {
            if (area.status() != TrackingAreaStatus.STALLED
                    || !isLeafArea(task.getInventoryDomain(), area)
                    || items.size() >= MAX_ATTENTION_ITEMS) continue;
            items.add(new TrackingResponses.AttentionItem(
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
        if (taskHasBeenRunningLongEnough) {
            for (TrackingResponses.Area area : areas) {
                if (area.status() != TrackingAreaStatus.NOT_STARTED
                        || !isLeafArea(task.getInventoryDomain(), area)
                        || items.size() >= MAX_ATTENTION_ITEMS) continue;
                items.add(new TrackingResponses.AttentionItem(
                        "AREA_NOT_STARTED:" + area.key(),
                        TrackingAttentionType.AREA_NOT_STARTED,
                        "Area has not started",
                        area.name(),
                        null, null, area.code(), area.name(), null, null,
                        task.getStartedAt(), false, null
                ));
            }
        }
        if (readyForReview && items.size() < MAX_ATTENTION_ITEMS) {
            items.add(new TrackingResponses.AttentionItem(
                    "READY_FOR_REVIEW:" + taskId,
                    TrackingAttentionType.READY_FOR_REVIEW,
                    "All expected items have been processed",
                    "The task is still IN_PROGRESS and can be submitted for review",
                    null, null, task.getTaskNumber(), null, null, null,
                    snapshot.events().lastActivityAt(), false, null
            ));
        }
        if (importFailed && items.size() < MAX_ATTENTION_ITEMS) {
            items.add(new TrackingResponses.AttentionItem(
                    "IMPORT_FAILED:" + taskId,
                    TrackingAttentionType.IMPORT_FAILED,
                    "Inventory import failed",
                    "Review the import activity in the task timeline before retrying",
                    null, null, task.getTaskNumber(), null, null, null,
                    task.getUpdatedAt(), false, null
            ));
        }
        if (highDuplicateRate && items.size() < MAX_ATTENTION_ITEMS) {
            int duplicateRate = (int) Math.round(
                    snapshot.events().duplicates() * 100.0 / snapshot.events().totalEvents());
            items.add(new TrackingResponses.AttentionItem(
                    "HIGH_DUPLICATE_RATE:" + taskId,
                    TrackingAttentionType.HIGH_DUPLICATE_RATE,
                    "Duplicate scan rate is high",
                    duplicateRate + "% of the recorded scan events are duplicates",
                    null, null, task.getTaskNumber(), null, null, null,
                    snapshot.events().lastActivityAt(), false, null
            ));
        }

        List<TrackingResponses.AttentionItem> limited = items.size() <= MAX_ATTENTION_ITEMS
                ? List.copyOf(items)
                : List.copyOf(items.subList(0, MAX_ATTENTION_ITEMS));
        return new TrackingResponses.Attention(
                summary,
                limited,
                totalIssues > limited.size(),
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public TrackingResponses.ImageFile image(Long taskId, Long scanId, User currentUser) {
        if (scanId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Scan ID is required");
        }
        InventoryTask task = taskScopeService.requireAccessibleTask(taskId, currentUser);
        TrackingResponses.ImageFile image = provider(task).image(taskId, scanId);
        if (image == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Scan image not found");
        }
        return image;
    }

    public Map<Long, TaskSnapshot> snapshots(List<InventoryTask> tasks) {
        Map<Long, TaskSnapshot> result = new LinkedHashMap<>();
        Map<InventoryDomain, List<InventoryTask>> byDomain = tasks.stream()
                .collect(Collectors.groupingBy(
                        InventoryTask::getInventoryDomain,
                        () -> new EnumMap<>(InventoryDomain.class),
                        Collectors.toList()
                ));
        byDomain.forEach((domain, domainTasks) -> {
            List<Long> ids = domainTasks.stream().map(InventoryTask::getId).toList();
            InventoryTrackingProvider provider = providerRegistry.get(domain);
            Map<Long, TrackingResponses.CurrentMetrics> current = provider.currentMetrics(ids);
            Map<Long, TrackingResponses.EventMetrics> events = provider.eventMetrics(ids);
            for (InventoryTask task : domainTasks) {
                result.put(task.getId(), new TaskSnapshot(
                        current.getOrDefault(task.getId(), TrackingResponses.CurrentMetrics.empty()),
                        events.getOrDefault(task.getId(), TrackingResponses.EventMetrics.empty())
                ));
            }
        });
        return result;
    }

    public List<TrackingResponses.TaskListItem> toListItems(List<InventoryTask> tasks) {
        Map<Long, TaskSnapshot> snapshots = snapshots(tasks);
        return tasks.stream().map(task -> toListItem(task, snapshots.get(task.getId()))).toList();
    }

    private TrackingResponses.TaskListItem toListItem(InventoryTask task, TaskSnapshot snapshot) {
        return new TrackingResponses.TaskListItem(
                task.getId(), task.getTaskNumber(), task.getTaskName(), companyRef(task),
                task.getInventoryDomain(), task.getStatus(), snapshot.current(), snapshot.events(),
                attentionCount(task, snapshot), isStalled(task, snapshot.events()),
                task.getStartedAt(), task.getReviewStartedAt(), task.getClosedAt(), task.getUpdatedAt()
        );
    }

    private long attentionCount(InventoryTask task, TaskSnapshot snapshot) {
        long count = snapshot.current().mismatched()
                + (task.isScanImageRequired() ? snapshot.current().acceptedWithoutImage() : 0)
                + snapshot.events().extraEvents()
                + snapshot.events().conflicts()
                + snapshot.current().acceptedWithNotes()
                + (task.getStatus() == InventoryTaskStatus.IMPORT_FAILED ? 1 : 0);
        if (isStalled(task, snapshot.events())) count++;
        if (task.getStatus() == InventoryTaskStatus.IN_PROGRESS
                && snapshot.current().totalExpected() > 0
                && snapshot.current().remaining() == 0) count++;
        if (isHighDuplicateRate(snapshot.events())) count++;
        return count;
    }

    private boolean isHighDuplicateRate(TrackingResponses.EventMetrics events) {
        return events.totalEvents() >= 20
                && events.duplicates() * 100.0 / events.totalEvents() >= 20.0;
    }

    private boolean isLeafArea(InventoryDomain domain, TrackingResponses.Area area) {
        return switch (domain) {
            case VEHICLE -> area.level() == com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaLevel.STORE;
            case ASSET -> area.level() == com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaLevel.PLACE;
            case SPARE_PART -> area.level() == com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaLevel.LOCATION;
        };
    }

    private boolean isStalled(InventoryTask task, TrackingResponses.EventMetrics events) {
        if (task.getStatus() != InventoryTaskStatus.IN_PROGRESS) return false;
        LocalDateTime reference = events.lastActivityAt() == null ? task.getStartedAt() : events.lastActivityAt();
        return reference != null && reference.isBefore(LocalDateTime.now().minusMinutes(STALLED_AFTER_MINUTES));
    }

    private TrackingResponses.CompanyRef companyRef(InventoryTask task) {
        return new TrackingResponses.CompanyRef(
                task.getCompany().getId(), task.getCompany().getCode(), task.getCompany().getName());
    }

    private InventoryTrackingProvider provider(InventoryTask task) {
        return providerRegistry.get(task.getInventoryDomain());
    }

    private TrackingResponses.AllowedActions allowedActions(InventoryTask task, User user) {
        boolean update = hasPermission(user, task.getInventoryDomain(), ActionPermission.UPDATE);
        boolean assign = hasPermission(user, task.getInventoryDomain(), ActionPermission.ASSIGN);
        boolean close = hasPermission(user, task.getInventoryDomain(), ActionPermission.CLOSE);
        InventoryTaskStatus status = task.getStatus();
        boolean closed = status == InventoryTaskStatus.COMPLETED || status == InventoryTaskStatus.CANCELLED;
        return new TrackingResponses.AllowedActions(
                update && !closed,
                assign && (status == InventoryTaskStatus.IMPORT_COMPLETED
                        || status == InventoryTaskStatus.READY_FOR_ASSIGNMENT
                        || status == InventoryTaskStatus.READY_TO_START
                        || status == InventoryTaskStatus.IN_PROGRESS
                        || status == InventoryTaskStatus.PAUSED),
                assign && (status == InventoryTaskStatus.IMPORT_COMPLETED
                        || status == InventoryTaskStatus.READY_FOR_ASSIGNMENT
                        || status == InventoryTaskStatus.DRAFT),
                update && (status == InventoryTaskStatus.IMPORT_COMPLETED
                        || status == InventoryTaskStatus.READY_FOR_ASSIGNMENT
                        || status == InventoryTaskStatus.READY_TO_START),
                update && status == InventoryTaskStatus.IN_PROGRESS,
                update && status == InventoryTaskStatus.PAUSED,
                update && status == InventoryTaskStatus.IN_PROGRESS,
                update && status == InventoryTaskStatus.UNDER_REVIEW,
                close && status == InventoryTaskStatus.UNDER_REVIEW,
                close && !closed
                        && status != InventoryTaskStatus.IMPORT_PENDING
                        && status != InventoryTaskStatus.IMPORT_IN_PROGRESS
        );
    }

    private boolean hasPermission(User user, InventoryDomain domain, ActionPermission action) {
        PermissionCode permission;
        if (domain == InventoryDomain.VEHICLE) {
            permission = switch (action) {
                case UPDATE -> PermissionCode.VEHICLE_TASK_UPDATE;
                case ASSIGN -> PermissionCode.VEHICLE_TASK_ASSIGN_USERS;
                case CLOSE -> PermissionCode.VEHICLE_TASK_CLOSE;
            };
        } else if (domain == InventoryDomain.ASSET) {
            permission = switch (action) {
                case UPDATE -> PermissionCode.ASSET_TASK_UPDATE;
                case ASSIGN -> PermissionCode.ASSET_TASK_ASSIGN_USERS;
                case CLOSE -> PermissionCode.ASSET_TASK_CLOSE;
            };
        } else {
            permission = switch (action) {
                case UPDATE -> PermissionCode.SPARE_PART_TASK_UPDATE;
                case ASSIGN -> PermissionCode.SPARE_PART_TASK_ASSIGN_USERS;
                case CLOSE -> PermissionCode.SPARE_PART_TASK_CLOSE;
            };
        }
        return user != null && user.hasPermission(permission);
    }

    private void appendResultAttention(
            List<TrackingResponses.AttentionItem> target,
            List<TrackingResponses.ResultItem> results,
            TrackingAttentionType type
    ) {
        for (TrackingResponses.ResultItem result : results) {
            if (target.size() >= MAX_ATTENTION_ITEMS) return;
            String title = switch (type) {
                case MISMATCH -> "Current result does not match the expected record";
                case MISSING_IMAGE -> "Required scan image is missing";
                case STAFF_NOTE -> "Inventory staff added a note";
                default -> type.name();
            };
            target.add(new TrackingResponses.AttentionItem(
                    type + ":ITEM:" + result.itemId(), type, title,
                    type == TrackingAttentionType.STAFF_NOTE ? result.notes() : result.status(),
                    result.itemId(), result.currentScanId(), result.code(),
                    joinArea(result.expectedArea(), result.expectedSubArea(), result.expectedLeafArea()),
                    joinArea(result.actualArea(), result.actualSubArea(), result.actualLeafArea()),
                    result.acceptedBy(), result.acceptedAt(), result.hasImage(), result.imageUrl()
            ));
        }
    }

    private TrackingResponses.ResultItem markRemainingAsNotFound(TrackingResponses.ResultItem item) {
        if (item.processed()) return item;
        return new TrackingResponses.ResultItem(
                item.itemId(), item.domain(), item.code(), item.secondaryCode(), item.description(),
                "NOT_FOUND", false, false,
                item.expectedArea(), item.expectedSubArea(), item.expectedLeafArea(),
                item.actualArea(), item.actualSubArea(), item.actualLeafArea(),
                item.expectedQuantity(), item.actualQuantity(), item.varianceQuantity(),
                item.acceptedBy(), item.acceptedAt(), item.currentScanId(), item.hasImage(),
                item.imageUrl(), item.notes()
        );
    }

    private void appendEventAttention(
            List<TrackingResponses.AttentionItem> target,
            List<TrackingResponses.ScanEvent> events,
            TrackingAttentionType type
    ) {
        for (TrackingResponses.ScanEvent event : events) {
            if (target.size() >= MAX_ATTENTION_ITEMS) return;
            String title = type == TrackingAttentionType.EXTRA
                    ? "Unexpected code was scanned"
                    : "The code was scanned in a conflicting location";
            target.add(new TrackingResponses.AttentionItem(
                    type + ":SCAN:" + event.scanId(), type, title,
                    event.details(), event.itemId(), event.scanId(), event.scannedCode(),
                    joinArea(event.expectedArea(), event.expectedSubArea(), event.expectedLeafArea()),
                    joinArea(event.actualArea(), event.actualSubArea(), event.actualLeafArea()),
                    event.scannedBy(), event.scannedAt(), event.hasImage(), event.imageUrl()
            ));
        }
    }

    private String joinArea(String first, String second, String third) {
        return Arrays.asList(first, second, third).stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" / "));
    }

    public record TaskSnapshot(
            TrackingResponses.CurrentMetrics current,
            TrackingResponses.EventMetrics events
    ) {}

    private enum ActionPermission { UPDATE, ASSIGN, CLOSE }
}
