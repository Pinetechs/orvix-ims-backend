package com.pinetechs.orvix.ims.inventory.review.service;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewIssueType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

record ReviewIssueCandidate(
        String issueKey,
        InventoryDomain domain,
        ReviewIssueType issueType,
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
        LocalDateTime detectedAt
) {}
