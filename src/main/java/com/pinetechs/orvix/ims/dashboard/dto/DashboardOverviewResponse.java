package com.pinetechs.orvix.ims.dashboard.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardOverviewResponse(
        LocalDateTime generatedAt,
        boolean operationalDashboardAvailable,
        TaskSummary taskSummary,
        ExecutionSummary executionSummary,
        AttentionSummary attentionSummary,
        List<DomainSummary> domainSummaries,
        List<TrackingResponses.TaskListItem> recentTasks,
        AdministrationSummary administrationSummary
) {
    public record TaskSummary(
            long active,
            long readyToStart,
            long inProgress,
            long paused,
            long underReview,
            long completedThisMonth
    ) {}

    public record ExecutionSummary(
            long expected,
            long processed,
            long remaining,
            long matched,
            long mismatched,
            int progressPercentage
    ) {}

    public record AttentionSummary(
            long totalIssues,
            long affectedTasks,
            long stalledTasks,
            long tasksWithMismatches,
            long tasksMissingRequiredImages,
            long importFailedTasks,
            long tasksWithHighDuplicateRate
    ) {}

    public record DomainSummary(
            InventoryDomain domain,
            long activeTasks,
            long expected,
            long processed,
            long remaining,
            long mismatched,
            int progressPercentage
    ) {}

    public record AdministrationSummary(
            long totalCompanies,
            long activeCompanies,
            long totalUsers,
            long activeUsers
    ) {}
}
