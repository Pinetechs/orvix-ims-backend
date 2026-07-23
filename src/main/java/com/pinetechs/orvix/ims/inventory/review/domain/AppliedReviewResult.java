package com.pinetechs.orvix.ims.inventory.review.domain;

public record AppliedReviewResult(
        Long previousScanId,
        Long acceptedScanId
) {}
