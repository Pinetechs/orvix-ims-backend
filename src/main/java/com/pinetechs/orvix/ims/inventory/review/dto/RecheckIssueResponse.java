package com.pinetechs.orvix.ims.inventory.review.dto;

import com.pinetechs.orvix.ims.inventory.review.enums.ReviewIssueStatus;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewIssueType;

public record RecheckIssueResponse(
        Long id,
        ReviewIssueType issueType,
        ReviewIssueStatus status,
        Long sourceScanId,
        Long currentScanId,
        String scanImageUrl
) {}
