package com.pinetechs.orvix.ims.inventory.review.service;

import com.pinetechs.orvix.ims.inventory.review.dto.*;
import com.pinetechs.orvix.ims.inventory.review.entity.*;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ReviewResponseMapper {

    public ReviewIssueResponse issue(InventoryReviewIssue issue) {
        return new ReviewIssueResponse(
                issue.getId(),
                issue.getInventoryDomain(),
                issue.getIssueType(),
                issue.getStatus(),
                issue.isBlocking(),
                issue.getItemId(),
                issue.getSourceScanId(),
                issue.getCurrentScanId(),
                issue.getItemCode(),
                issue.getSecondaryCode(),
                issue.getItemDescription(),
                issue.getWorkAreaKey(),
                issue.getWorkAreaLabel(),
                issue.getExpectedArea(),
                issue.getExpectedSubArea(),
                issue.getExpectedLeafArea(),
                issue.getActualArea(),
                issue.getActualSubArea(),
                issue.getActualLeafArea(),
                issue.getExpectedQuantity(),
                issue.getActualQuantity(),
                issue.getVarianceQuantity(),
                issue.getDetectedAt(),
                issue.getResolvedAt(),
                ReviewUserResponse.from(issue.getResolvedBy())
        );
    }

    public ReviewDecisionResponse decision(InventoryReviewDecision decision) {
        List<Long> issueIds = decision.getIssues().stream()
                .map(InventoryReviewIssue::getId)
                .sorted()
                .toList();
        return new ReviewDecisionResponse(
                decision.getId(),
                issueIds,
                decision.getRecheckItem() == null ? null : decision.getRecheckItem().getId(),
                decision.getDecisionType(),
                decision.getReasonCode(),
                decision.getNote(),
                ReviewUserResponse.from(decision.getDecidedBy()),
                decision.getDecidedAt(),
                decision.getPreviousScanId(),
                decision.getAcceptedScanId()
        );
    }

    public RecheckRequestResponse recheckRequest(InventoryRecheckRequest request) {
        List<RecheckItemResponse> items = request.getItems().stream()
                .sorted(Comparator.comparing(InventoryRecheckItem::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::recheckItem)
                .toList();
        return new RecheckRequestResponse(
                request.getId(),
                request.getRequestNumber(),
                request.getInventoryTask().getId(),
                request.getInventoryTask().getTaskNumber(),
                request.getInventoryTask().getTaskName(),
                request.getInventoryDomain(),
                request.getStatus(),
                request.getWorkAreaKey(),
                request.getWorkAreaLabel(),
                request.getInstructions(),
                request.isImageRequired(),
                request.getDueAt(),
                ReviewUserResponse.from(request.getAssignedTo()),
                ReviewUserResponse.from(request.getRequestedBy()),
                items,
                request.getStartedAt(),
                request.getSubmittedAt(),
                request.getCompletedAt(),
                request.getCancelledAt(),
                request.getCancellationReason(),
                request.getCreatedAt()
        );
    }

    private RecheckItemResponse recheckItem(InventoryRecheckItem item) {
        List<InventoryReviewIssue> issues = item.getIssues().stream()
                .sorted(Comparator.comparing(InventoryReviewIssue::getId))
                .toList();
        InventoryReviewIssue primary = issues.isEmpty() ? null : issues.get(0);

        List<RecheckIssueResponse> issueResponses = issues.stream()
                .map(issue -> new RecheckIssueResponse(
                        issue.getId(),
                        issue.getIssueType(),
                        issue.getStatus(),
                        issue.getSourceScanId(),
                        issue.getCurrentScanId(),
                        scanImageUrl(issue)))
                .toList();

        return new RecheckItemResponse(
                item.getId(),
                item.getStatus(),
                issueResponses,
                item.getReferenceItemId(),
                item.getResolvedItemId(),
                item.getPreviousScanId(),
                item.getAcceptedScanId(),
                primary == null ? item.getScannedCode() : primary.getItemCode(),
                primary == null ? null : primary.getItemDescription(),
                primary == null ? null : location(
                        primary.getExpectedArea(),
                        primary.getExpectedSubArea(),
                        primary.getExpectedLeafArea()),
                primary == null ? null : previousResult(primary),
                issues.stream()
                        .map(InventoryReviewIssue::getExpectedQuantity)
                        .filter(java.util.Objects::nonNull)
                        .findFirst()
                        .orElse(null),
                item.getResult(),
                item.getScannedCode(),
                item.getBranchId(),
                item.getLocationId(),
                item.getFloorId(),
                item.getPlaceId(),
                item.getCountedQuantity(),
                item.getReasonCode(),
                item.getNote(),
                item.getEvidenceImage() != null,
                evidenceImageUrl(item),
                item.getSubmittedAt(),
                item.getReviewedAt(),
                ReviewUserResponse.from(item.getReviewedBy())
        );
    }

    private String evidenceImageUrl(InventoryRecheckItem item) {
        if (item.getEvidenceImage() == null
                || item.getRecheckRequest() == null
                || item.getRecheckRequest().getInventoryTask() == null) {
            return null;
        }
        return "/api/inventory/tasks/"
                + item.getRecheckRequest().getInventoryTask().getId()
                + "/review/rechecks/"
                + item.getRecheckRequest().getId()
                + "/items/"
                + item.getId()
                + "/evidence";
    }

    private String previousResult(InventoryReviewIssue issue) {
        if (issue.getActualQuantity() != null) {
            return issue.getActualQuantity().stripTrailingZeros().toPlainString();
        }
        return location(issue.getActualArea(), issue.getActualSubArea(), issue.getActualLeafArea());
    }

    private String scanImageUrl(InventoryReviewIssue issue) {
        Long scanId = issue.getSourceScanId() != null
                ? issue.getSourceScanId()
                : issue.getCurrentScanId();
        if (scanId == null || issue.getInventoryTask() == null) return null;
        return "/api/app/v1/tasks/"
                + issue.getInventoryTask().getId()
                + "/scans/"
                + scanId
                + "/image";
    }

    private String location(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            if (!result.isEmpty()) result.append(" / ");
            result.append(value.trim());
        }
        return result.isEmpty() ? null : result.toString();
    }
}
