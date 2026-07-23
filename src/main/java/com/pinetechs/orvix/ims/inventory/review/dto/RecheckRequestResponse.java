package com.pinetechs.orvix.ims.inventory.review.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public record RecheckRequestResponse(
        Long id,
        String requestNumber,
        Long taskId,
        String taskNumber,
        String taskName,
        InventoryDomain inventoryDomain,
        RecheckRequestStatus status,
        String workAreaKey,
        String workAreaLabel,
        String instructions,
        boolean imageRequired,
        LocalDateTime dueAt,
        ReviewUserResponse assignedTo,
        ReviewUserResponse requestedBy,
        List<RecheckItemResponse> items,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        String cancellationReason,
        LocalDateTime createdAt
) {}
