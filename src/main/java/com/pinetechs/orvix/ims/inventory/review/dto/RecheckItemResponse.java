package com.pinetechs.orvix.ims.inventory.review.dto;

import com.pinetechs.orvix.ims.inventory.review.enums.RecheckItemStatus;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckResult;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewReasonCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RecheckItemResponse(
        Long id,
        RecheckItemStatus status,
        List<RecheckIssueResponse> issues,
        Long referenceItemId,
        Long resolvedItemId,
        Long previousScanId,
        Long acceptedScanId,
        String itemCode,
        String itemDescription,
        String expectedLocation,
        String previousResult,
        BigDecimal expectedQuantity,
        RecheckResult result,
        String scannedCode,
        Long branchId,
        Long locationId,
        Long floorId,
        Long placeId,
        BigDecimal countedQuantity,
        ReviewReasonCode reasonCode,
        String note,
        boolean hasEvidenceImage,
        String evidenceImageUrl,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        ReviewUserResponse reviewedBy
) {}
