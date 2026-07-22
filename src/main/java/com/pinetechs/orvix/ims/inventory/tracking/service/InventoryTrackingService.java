package com.pinetechs.orvix.ims.inventory.tracking.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingSnapshot;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingResultFilter;
import com.pinetechs.orvix.ims.inventory.tracking.policy.TrackingActionPolicy;
import com.pinetechs.orvix.ims.inventory.tracking.policy.TrackingStatusPolicy;
import com.pinetechs.orvix.ims.inventory.tracking.provider.InventoryTrackingProvider;
import com.pinetechs.orvix.ims.inventory.tracking.provider.InventoryTrackingProviderRegistry;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class InventoryTrackingService {

    private final TrackingTaskScopeService taskScopeService;
    private final InventoryTrackingProviderRegistry providerRegistry;
    private final TrackingSnapshotService snapshotService;
    private final TrackingAttentionService attentionService;
    private final TrackingDurationService durationService;
    private final TrackingStatusPolicy statusPolicy;
    private final TrackingActionPolicy actionPolicy;

    public InventoryTrackingService(
            TrackingTaskScopeService taskScopeService,
            InventoryTrackingProviderRegistry providerRegistry,
            TrackingSnapshotService snapshotService,
            TrackingAttentionService attentionService,
            TrackingDurationService durationService,
            TrackingStatusPolicy statusPolicy,
            TrackingActionPolicy actionPolicy
    ) {
        this.taskScopeService = taskScopeService;
        this.providerRegistry = providerRegistry;
        this.snapshotService = snapshotService;
        this.attentionService = attentionService;
        this.durationService = durationService;
        this.statusPolicy = statusPolicy;
        this.actionPolicy = actionPolicy;
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
                currentUser,
                search,
                companyId,
                domain,
                status,
                pageable
        );
        Map<Long, TrackingSnapshot> snapshots = snapshotService.load(tasks.getContent());
        List<TrackingResponses.TaskListItem> content = tasks.getContent().stream()
                .map(task -> toListItem(task, snapshots.get(task.getId())))
                .toList();
        return new PageImpl<>(content, pageable, tasks.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TrackingResponses.TaskOverview overview(Long taskId, User currentUser) {
        InventoryTask task = taskScopeService.requireAccessibleTask(taskId, currentUser);
        TrackingSnapshot snapshot = snapshotService.load(task);

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
                statusPolicy.isTaskStalled(task, snapshot.events()),
                task.getStartedAt(),
                task.getPausedAt(),
                task.getPauseReason(),
                task.getReviewStartedAt(),
                task.getClosedAt(),
                LocalDateTime.now(),
                actionPolicy.allowedActions(task, currentUser)
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
        TrackingResultFilter resolvedFilter = filter == null
                ? TrackingResultFilter.ALL
                : filter;

        if (resolvedFilter == TrackingResultFilter.MISSING_IMAGE
                && !task.isScanImageRequired()) {
            return Page.empty(pageable);
        }

        Page<TrackingResponses.ResultItem> results = provider(task).results(
                taskId,
                resolvedFilter,
                search,
                pageable
        );

        if (shouldMarkRemainingAsNotFound(task)) {
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
        TrackingSnapshot snapshot = snapshotService.load(task);
        return attentionService.build(task, provider, snapshot);
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

    public Map<Long, TrackingSnapshot> snapshots(List<InventoryTask> tasks) {
        return snapshotService.load(tasks);
    }

    public List<TrackingResponses.TaskListItem> toListItems(List<InventoryTask> tasks) {
        Map<Long, TrackingSnapshot> snapshots = snapshotService.load(tasks);
        return tasks.stream()
                .map(task -> toListItem(task, snapshots.get(task.getId())))
                .toList();
    }

    private TrackingResponses.TaskListItem toListItem(
            InventoryTask task,
            TrackingSnapshot snapshot
    ) {
        return new TrackingResponses.TaskListItem(
                task.getId(),
                task.getTaskNumber(),
                task.getTaskName(),
                companyRef(task),
                task.getInventoryDomain(),
                task.getStatus(),
                snapshot.current(),
                snapshot.events(),
                attentionCount(task, snapshot),
                statusPolicy.isTaskStalled(task, snapshot.events()),
                task.getStartedAt(),
                task.getReviewStartedAt(),
                task.getClosedAt(),
                task.getUpdatedAt()
        );
    }

    private long attentionCount(InventoryTask task, TrackingSnapshot snapshot) {
        long count = snapshot.current().mismatched()
                + requiredMissingImages(task, snapshot)
                + snapshot.events().extraEvents()
                + snapshot.events().conflicts()
                + snapshot.current().acceptedWithNotes();

        if (task.getStatus() == InventoryTaskStatus.IMPORT_FAILED) {
            count++;
        }
        if (statusPolicy.isTaskStalled(task, snapshot.events())) {
            count++;
        }
        if (statusPolicy.isReadyForReview(task, snapshot.current())) {
            count++;
        }
        if (statusPolicy.isHighDuplicateRate(snapshot.events())) {
            count++;
        }
        return count;
    }

    private long requiredMissingImages(InventoryTask task, TrackingSnapshot snapshot) {
        return task.isScanImageRequired()
                ? snapshot.current().acceptedWithoutImage()
                : 0;
    }

    private boolean shouldMarkRemainingAsNotFound(InventoryTask task) {
        boolean reviewOrCompleted = task.getStatus() == InventoryTaskStatus.UNDER_REVIEW
                || task.getStatus() == InventoryTaskStatus.COMPLETED;
        return reviewOrCompleted && task.getInventoryDomain() != InventoryDomain.SPARE_PART;
    }

    private TrackingResponses.ResultItem markRemainingAsNotFound(
            TrackingResponses.ResultItem item
    ) {
        if (item.processed()) {
            return item;
        }
        return new TrackingResponses.ResultItem(
                item.itemId(),
                item.domain(),
                item.code(),
                item.secondaryCode(),
                item.description(),
                "NOT_FOUND",
                false,
                false,
                item.expectedArea(),
                item.expectedSubArea(),
                item.expectedLeafArea(),
                item.actualArea(),
                item.actualSubArea(),
                item.actualLeafArea(),
                item.expectedQuantity(),
                item.actualQuantity(),
                item.varianceQuantity(),
                item.acceptedBy(),
                item.acceptedAt(),
                item.currentScanId(),
                item.hasImage(),
                item.imageUrl(),
                item.notes()
        );
    }

    private TrackingResponses.CompanyRef companyRef(InventoryTask task) {
        return new TrackingResponses.CompanyRef(
                task.getCompany().getId(),
                task.getCompany().getCode(),
                task.getCompany().getName()
        );
    }

    private InventoryTrackingProvider provider(InventoryTask task) {
        return providerRegistry.get(task.getInventoryDomain());
    }
}
