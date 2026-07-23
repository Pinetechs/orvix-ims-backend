package com.pinetechs.orvix.ims.inventory.review.dto;

import java.util.List;

public record ReviewIssueDetailsResponse(
        ReviewIssueResponse issue,
        List<ReviewDecisionResponse> decisions
) {}
