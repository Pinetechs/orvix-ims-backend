package com.pinetechs.orvix.ims.inventory.review.dto;

import com.pinetechs.orvix.ims.inventory.review.enums.ReviewDecisionType;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewReasonCode;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewDecisionResponse(
        Long id,
        List<Long> issueIds,
        Long recheckItemId,
        ReviewDecisionType decision,
        ReviewReasonCode reasonCode,
        String note,
        ReviewUserResponse decidedBy,
        LocalDateTime decidedAt,
        Long previousScanId,
        Long acceptedScanId
) {}
