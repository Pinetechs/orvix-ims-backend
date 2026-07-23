package com.pinetechs.orvix.ims.inventory.review.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewIssueStatus;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewIssueType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReviewIssueResponse(
        Long id,
        InventoryDomain inventoryDomain,
        ReviewIssueType issueType,
        ReviewIssueStatus status,
        boolean blocking,
        Long itemId,
        Long sourceScanId,
        Long currentScanId,
        String itemCode,
        String secondaryCode,
        String itemDescription,
        String workAreaKey,
        String workAreaLabel,
        String expectedArea,
        String expectedSubArea,
        String expectedLeafArea,
        String actualArea,
        String actualSubArea,
        String actualLeafArea,
        BigDecimal expectedQuantity,
        BigDecimal actualQuantity,
        BigDecimal varianceQuantity,
        LocalDateTime detectedAt,
        LocalDateTime resolvedAt,
        ReviewUserResponse resolvedBy
) {}
