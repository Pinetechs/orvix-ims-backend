package com.pinetechs.orvix.ims.inventory.review.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewIssueStatus;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewIssueType;

import java.util.Map;

public record ReviewCenterSummaryResponse(
        Long taskId,
        String taskNumber,
        InventoryDomain inventoryDomain,
        Map<ReviewIssueStatus, Long> issuesByStatus,
        Map<ReviewIssueType, Long> openIssuesByType,
        long blockingOpenIssues,
        long activeRecheckRequests,
        boolean canComplete
) {}
