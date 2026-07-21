package com.pinetechs.orvix.ims.dashboard.service;

import com.pinetechs.orvix.ims.company.repository.CompanyRepository;
import com.pinetechs.orvix.ims.dashboard.dto.DashboardOverviewResponse;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.service.InventoryTrackingService;
import com.pinetechs.orvix.ims.inventory.tracking.service.TrackingTaskScopeService;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import com.pinetechs.orvix.ims.user.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final List<InventoryTaskStatus> ACTIVE_STATUSES = List.of(
            InventoryTaskStatus.CREATED,
            InventoryTaskStatus.IMPORT_PENDING,
            InventoryTaskStatus.IMPORT_IN_PROGRESS,
            InventoryTaskStatus.IMPORT_FAILED,
            InventoryTaskStatus.IMPORT_COMPLETED,
            InventoryTaskStatus.READY_FOR_ASSIGNMENT,
            InventoryTaskStatus.READY_TO_START,
            InventoryTaskStatus.IN_PROGRESS,
            InventoryTaskStatus.PAUSED,
            InventoryTaskStatus.UNDER_REVIEW
    );

    private final TrackingTaskScopeService taskScopeService;
    private final InventoryTrackingService trackingService;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public DashboardService(
            TrackingTaskScopeService taskScopeService,
            InventoryTrackingService trackingService,
            CompanyRepository companyRepository,
            UserRepository userRepository
    ) {
        this.taskScopeService = taskScopeService;
        this.trackingService = trackingService;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(User currentUser) {
        boolean operational = taskScopeService.hasOperationalDashboardAccess(currentUser);
        DashboardOverviewResponse.AdministrationSummary administration = administrationSummary(currentUser);
        if (!operational) {
            return new DashboardOverviewResponse(
                    LocalDateTime.now(),
                    false,
                    new DashboardOverviewResponse.TaskSummary(0, 0, 0, 0, 0, 0),
                    new DashboardOverviewResponse.ExecutionSummary(0, 0, 0, 0, 0, 0),
                    new DashboardOverviewResponse.AttentionSummary(0, 0, 0, 0, 0, 0, 0),
                    List.of(),
                    List.of(),
                    administration
            );
        }

        List<InventoryTask> activeTasks = taskScopeService.findAccessibleTasks(
                currentUser,
                ACTIVE_STATUSES,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );
        Map<Long, InventoryTrackingService.TaskSnapshot> snapshots = trackingService.snapshots(activeTasks);

        long expected = 0;
        long processed = 0;
        long matched = 0;
        long mismatched = 0;
        long totalIssues = 0;
        long affectedTasks = 0;
        long stalledTasks = 0;
        long tasksWithMismatches = 0;
        long tasksMissingImages = 0;
        long importFailedTasks = 0;
        long highDuplicateRateTasks = 0;

        Map<InventoryDomain, DomainAccumulator> domains = new EnumMap<>(InventoryDomain.class);
        for (InventoryTask task : activeTasks) {
            InventoryTrackingService.TaskSnapshot snapshot = snapshots.get(task.getId());
            TrackingResponses.CurrentMetrics current = snapshot.current();
            TrackingResponses.EventMetrics events = snapshot.events();
            expected += current.totalExpected();
            processed += current.processedExpected();
            matched += current.matched();
            mismatched += current.mismatched();

            long missingImages = task.isScanImageRequired() ? current.acceptedWithoutImage() : 0;
            boolean importFailed = task.getStatus() == InventoryTaskStatus.IMPORT_FAILED;
            boolean stalled = isStalled(task, events);
            boolean readyForReview = task.getStatus() == InventoryTaskStatus.IN_PROGRESS
                    && current.totalExpected() > 0
                    && current.remaining() == 0;
            boolean highDuplicateRate = isHighDuplicateRate(events);
            long issues = current.mismatched() + missingImages + events.extraEvents()
                    + events.conflicts() + current.acceptedWithNotes();
            if (importFailed) {
                issues++;
                importFailedTasks++;
            }
            if (stalled) issues++;
            if (readyForReview) issues++;
            if (highDuplicateRate) {
                issues++;
                highDuplicateRateTasks++;
            }
            totalIssues += issues;
            if (issues > 0) affectedTasks++;
            if (current.mismatched() > 0) tasksWithMismatches++;
            if (missingImages > 0) tasksMissingImages++;
            if (stalled) stalledTasks++;

            domains.computeIfAbsent(task.getInventoryDomain(), ignored -> new DomainAccumulator())
                    .add(current);
        }

        List<DashboardOverviewResponse.DomainSummary> domainSummaries = new ArrayList<>();
        for (InventoryDomain domain : InventoryDomain.values()) {
            DomainAccumulator accumulator = domains.get(domain);
            if (accumulator == null) continue;
            domainSummaries.add(accumulator.toResponse(domain));
        }

        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        long completedThisMonth = taskScopeService.countAccessibleCompletedSince(
                currentUser, firstDayOfMonth.atStartOfDay());

        DashboardOverviewResponse.TaskSummary taskSummary = new DashboardOverviewResponse.TaskSummary(
                activeTasks.size(),
                countStatus(activeTasks, InventoryTaskStatus.READY_TO_START),
                countStatus(activeTasks, InventoryTaskStatus.IN_PROGRESS),
                countStatus(activeTasks, InventoryTaskStatus.PAUSED),
                countStatus(activeTasks, InventoryTaskStatus.UNDER_REVIEW),
                completedThisMonth
        );
        DashboardOverviewResponse.ExecutionSummary execution = new DashboardOverviewResponse.ExecutionSummary(
                expected, processed, Math.max(expected - processed, 0), matched, mismatched,
                percentage(processed, expected)
        );
        DashboardOverviewResponse.AttentionSummary attention = new DashboardOverviewResponse.AttentionSummary(
                totalIssues, affectedTasks, stalledTasks, tasksWithMismatches,
                tasksMissingImages, importFailedTasks, highDuplicateRateTasks
        );

        List<TrackingResponses.TaskListItem> recentTasks = trackingService.toListItems(
                activeTasks.stream().limit(6).toList());
        return new DashboardOverviewResponse(
                LocalDateTime.now(), true, taskSummary, execution, attention,
                domainSummaries, recentTasks, administration
        );
    }

    private DashboardOverviewResponse.AdministrationSummary administrationSummary(User user) {
        if (user == null || (!user.hasPermission(PermissionCode.COMPANY_VIEW)
                && !user.hasPermission(PermissionCode.USER_VIEW))) {
            return null;
        }
        return new DashboardOverviewResponse.AdministrationSummary(
                companyRepository.count(),
                companyRepository.countByActiveTrue(),
                userRepository.countByDeletedFalse(),
                userRepository.countByDeletedFalseAndEnabledTrue()
        );
    }

    private long countStatus(List<InventoryTask> tasks, InventoryTaskStatus status) {
        return tasks.stream().filter(task -> task.getStatus() == status).count();
    }

    private boolean isStalled(InventoryTask task, TrackingResponses.EventMetrics events) {
        if (task.getStatus() != InventoryTaskStatus.IN_PROGRESS) return false;
        LocalDateTime reference = events.lastActivityAt() == null ? task.getStartedAt() : events.lastActivityAt();
        return reference != null && reference.isBefore(LocalDateTime.now().minusMinutes(30));
    }

    private boolean isHighDuplicateRate(TrackingResponses.EventMetrics events) {
        return events.totalEvents() >= 20
                && events.duplicates() * 100.0 / events.totalEvents() >= 20.0;
    }

    private int percentage(long value, long total) {
        if (total <= 0) return 0;
        return (int) Math.min(Math.round(value * 100.0 / total), 100);
    }

    private static final class DomainAccumulator {
        private long tasks;
        private long expected;
        private long processed;
        private long mismatched;

        private DomainAccumulator add(TrackingResponses.CurrentMetrics metrics) {
            tasks++;
            expected += metrics.totalExpected();
            processed += metrics.processedExpected();
            mismatched += metrics.mismatched();
            return this;
        }

        private DashboardOverviewResponse.DomainSummary toResponse(InventoryDomain domain) {
            return new DashboardOverviewResponse.DomainSummary(
                    domain, tasks, expected, processed, Math.max(expected - processed, 0),
                    mismatched, expected == 0 ? 0 : (int) Math.min(Math.round(processed * 100.0 / expected), 100)
            );
        }
    }
}
