package com.pinetechs.orvix.ims.inventory.review.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.review.domain.AppliedReviewResult;
import com.pinetechs.orvix.ims.inventory.review.domain.ReviewDomainHandlerRegistry;
import com.pinetechs.orvix.ims.inventory.review.dto.*;
import com.pinetechs.orvix.ims.inventory.review.entity.*;
import com.pinetechs.orvix.ims.inventory.review.enums.*;
import com.pinetechs.orvix.ims.inventory.review.repository.*;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryScanRepository;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.enums.InventoryTaskActivityType;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskActivityService;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class ReviewDecisionService {

    private final InventoryReviewIssueRepository issueRepository;
    private final InventoryRecheckItemRepository recheckItemRepository;
    private final InventoryRecheckRequestRepository recheckRequestRepository;
    private final InventoryReviewDecisionRepository decisionRepository;
    private final SparePartInventoryScanRepository sparePartScanRepository;
    private final ReviewDomainHandlerRegistry handlerRegistry;
    private final ReviewResponseMapper mapper;
    private final AccessPolicyService accessPolicyService;
    private final InventoryTaskActivityService taskActivityService;

    public ReviewDecisionService(
            InventoryReviewIssueRepository issueRepository,
            InventoryRecheckItemRepository recheckItemRepository,
            InventoryRecheckRequestRepository recheckRequestRepository,
            InventoryReviewDecisionRepository decisionRepository,
            SparePartInventoryScanRepository sparePartScanRepository,
            ReviewDomainHandlerRegistry handlerRegistry,
            ReviewResponseMapper mapper,
            AccessPolicyService accessPolicyService,
            InventoryTaskActivityService taskActivityService
    ) {
        this.issueRepository = issueRepository;
        this.recheckItemRepository = recheckItemRepository;
        this.recheckRequestRepository = recheckRequestRepository;
        this.decisionRepository = decisionRepository;
        this.sparePartScanRepository = sparePartScanRepository;
        this.handlerRegistry = handlerRegistry;
        this.mapper = mapper;
        this.accessPolicyService = accessPolicyService;
        this.taskActivityService = taskActivityService;
    }

    @Transactional
    public ReviewIssueDetailsResponse decideIssue(
            Long taskId,
            Long issueId,
            ReviewDecisionRequest input,
            User supervisor
    ) {
        InventoryReviewIssue issue = issueRepository.findForUpdate(taskId, issueId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Review issue not found"));
        InventoryTask task = issue.getInventoryTask();
        assertCanManage(task, supervisor);
        requireUnderReview(task);
        validateDecision(input);

        if (issue.getStatus() != ReviewIssueStatus.OPEN) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Only an open issue can receive a direct decision");
        }
        if (input.getDecision() == ReviewDecisionType.ACCEPT_RECHECK_RESULT
                || input.getDecision() == ReviewDecisionType.REQUEST_ANOTHER_RECHECK) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "This decision requires a submitted recheck item");
        }
        validateIssueDecision(issue, input.getDecision());

        LocalDateTime now = LocalDateTime.now();
        issue.resolve(supervisor, now);
        resolveSourceScan(issue, supervisor, now);

        InventoryReviewDecision decision = newDecision(
                Set.of(issue), null, input, supervisor, now);
        decisionRepository.save(decision);
        issueRepository.save(issue);
        recordDecisionActivity(task, input.getDecision(), supervisor, 1);

        return new ReviewIssueDetailsResponse(
                mapper.issue(issue),
                decisionRepository.findByIssueId(issueId).stream()
                        .map(mapper::decision)
                        .toList()
        );
    }

    @Transactional
    public RecheckRequestResponse decideRecheckItem(
            Long taskId,
            Long requestId,
            Long itemId,
            ReviewDecisionRequest input,
            User supervisor
    ) {
        InventoryRecheckItem item = recheckItemRepository
                .findForUpdate(requestId, itemId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Recheck item not found"));
        InventoryRecheckRequest request = item.getRecheckRequest();
        InventoryTask task = request.getInventoryTask();
        if (!taskId.equals(task.getId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "Recheck item not found");
        }
        assertCanManage(task, supervisor);
        requireUnderReview(task);
        validateDecision(input);

        if (item.getStatus() != RecheckItemStatus.SUBMITTED) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Only a submitted recheck item can receive a decision");
        }

        LocalDateTime now = LocalDateTime.now();
        Set<InventoryReviewIssue> affectedIssues =
                new LinkedHashSet<>(lockIssues(task.getId(), item.getIssues()));
        AppliedReviewResult applied = null;

        if (input.getDecision() == ReviewDecisionType.ACCEPT_RECHECK_RESULT) {
            applied = handlerRegistry.get(task.getInventoryDomain())
                    .accept(task, item, request.getAssignedTo());
            Long resolvedItemId = item.getResolvedItemId() != null
                    ? item.getResolvedItemId()
                    : item.getReferenceItemId();
            if (resolvedItemId != null) {
                Set<InventoryReviewIssue> siblingIssues = new LinkedHashSet<>(
                        issueRepository.findByInventoryTaskIdAndItemIdAndStatusIn(
                                task.getId(),
                                resolvedItemId,
                                ReviewCenterService.OPEN_ISSUE_STATUSES));
                affectedIssues.addAll(lockIssues(task.getId(), siblingIssues));
            }
            item.accept(applied.acceptedScanId(), supervisor, now);
            resolveIssues(affectedIssues, supervisor, now);
        } else if (input.getDecision() == ReviewDecisionType.REQUEST_ANOTHER_RECHECK) {
            item.reject(supervisor, now);
            affectedIssues.stream()
                    .filter(issue -> issue.getStatus().isOpen())
                    .forEach(InventoryReviewIssue::markOpen);
        } else {
            validateIssueDecisions(affectedIssues, input.getDecision());
            item.reject(supervisor, now);
            resolveIssues(affectedIssues, supervisor, now);
        }

        InventoryReviewDecision decision = newDecision(
                affectedIssues, item, input, supervisor, now);
        if (applied != null) {
            decision.setPreviousScanId(applied.previousScanId());
            decision.setAcceptedScanId(applied.acceptedScanId());
        } else {
            decision.setPreviousScanId(item.getPreviousScanId());
        }
        decisionRepository.save(decision);
        recheckItemRepository.save(item);
        issueRepository.saveAll(affectedIssues);
        updateRequestStatus(request, now);
        recheckRequestRepository.save(request);
        recordDecisionActivity(
                task, input.getDecision(), supervisor, affectedIssues.size());
        return mapper.recheckRequest(request);
    }

    private void resolveIssues(
            Set<InventoryReviewIssue> issues,
            User supervisor,
            LocalDateTime now
    ) {
        for (InventoryReviewIssue issue : issues) {
            if (!issue.getStatus().isOpen()) continue;
            issue.resolve(supervisor, now);
            resolveSourceScan(issue, supervisor, now);
        }
    }

    private Set<InventoryReviewIssue> lockIssues(
            Long taskId,
            Set<InventoryReviewIssue> issues
    ) {
        if (issues.isEmpty()) return new LinkedHashSet<>();
        Set<Long> ids = new LinkedHashSet<>();
        issues.forEach(issue -> ids.add(issue.getId()));
        return new LinkedHashSet<>(
                issueRepository.findAllForUpdate(taskId, ids));
    }

    private void resolveSourceScan(
            InventoryReviewIssue issue,
            User supervisor,
            LocalDateTime now
    ) {
        if (issue.getInventoryDomain() != InventoryDomain.SPARE_PART
                || (issue.getSourceScanId() == null
                    && issue.getCurrentScanId() == null)) {
            return;
        }
        Long scanId = issue.getSourceScanId() != null
                ? issue.getSourceScanId()
                : issue.getCurrentScanId();
        long remainingIssues = issueRepository.countOpenByScanReference(
                issue.getInventoryTask().getId(),
                scanId,
                ReviewCenterService.OPEN_ISSUE_STATUSES
        );
        if (remainingIssues > 0) return;
        sparePartScanRepository
                .findByIdAndInventoryTaskId(
                        scanId,
                        issue.getInventoryTask().getId())
                .ifPresent(scan -> {
                    scan.setReviewResolvedAt(now);
                    scan.setReviewResolvedBy(supervisor);
                    sparePartScanRepository.save(scan);
                });
    }

    private InventoryReviewDecision newDecision(
            Set<InventoryReviewIssue> issues,
            InventoryRecheckItem item,
            ReviewDecisionRequest input,
            User supervisor,
            LocalDateTime now
    ) {
        InventoryReviewDecision decision = new InventoryReviewDecision();
        decision.getIssues().addAll(issues);
        decision.setRecheckItem(item);
        decision.setDecisionType(input.getDecision());
        decision.setReasonCode(input.getReasonCode());
        decision.setNote(trim(input.getNote(), 1000));
        decision.setDecidedBy(supervisor);
        decision.setDecidedAt(now);
        return decision;
    }

    private void updateRequestStatus(
            InventoryRecheckRequest request,
            LocalDateTime now
    ) {
        boolean allFinal = request.getItems().stream()
                .allMatch(item -> item.getStatus().isFinal());
        if (allFinal) {
            request.complete(now);
            return;
        }
        boolean hasSubmitted = request.getItems().stream()
                .anyMatch(item -> item.getStatus() == RecheckItemStatus.SUBMITTED);
        if (hasSubmitted) {
            request.markSubmitted(now);
        } else {
            request.start(now);
        }
    }

    private void validateDecision(ReviewDecisionRequest input) {
        if (input == null || input.getDecision() == null) {
            badRequest("Review decision is required");
        }
        if (input.getReasonCode() == null) {
            badRequest("Review reason code is required");
        }
        String note = trim(input.getNote(), 1000);
        if ((input.getReasonCode() == ReviewReasonCode.OTHER
                || input.getDecision() == ReviewDecisionType.REQUEST_ANOTHER_RECHECK)
                && note == null) {
            badRequest("A note is required for this review decision");
        }
    }

    private void validateIssueDecisions(
            Set<InventoryReviewIssue> issues,
            ReviewDecisionType decision
    ) {
        for (InventoryReviewIssue issue : issues) {
            validateIssueDecision(issue, decision);
        }
    }

    private void validateIssueDecision(
            InventoryReviewIssue issue,
            ReviewDecisionType decision
    ) {
        if (decision == ReviewDecisionType.CONFIRM_EXTRA
                && issue.getIssueType() != ReviewIssueType.EXTRA
                && issue.getIssueType() != ReviewIssueType.AMBIGUOUS) {
            badRequest("CONFIRM_EXTRA is valid only for an extra or ambiguous issue");
        }
        if ((decision == ReviewDecisionType.CONFIRM_MISSING
                || decision == ReviewDecisionType.CONFIRM_DESTROYED)
                && issue.getItemId() == null) {
            badRequest("The selected issue does not reference an expected item");
        }
    }

    private void recordDecisionActivity(
            InventoryTask task,
            ReviewDecisionType decision,
            User supervisor,
            int issueCount
    ) {
        taskActivityService.record(
                task,
                InventoryTaskActivityType.REVIEW_DECISION_RECORDED,
                task.getStatus(),
                task.getStatus(),
                supervisor,
                null,
                decision + " applied to " + issueCount + " issue(s)"
        );
    }

    private void assertCanManage(InventoryTask task, User user) {
        accessPolicyService.assertCanUpdateTask(
                user, task.getCompany().getId(), task.getInventoryDomain());
    }

    private void requireUnderReview(InventoryTask task) {
        if (task.getStatus() != InventoryTaskStatus.UNDER_REVIEW) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Review decisions are allowed only while the task is UNDER_REVIEW");
        }
    }

    private String trim(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > max) {
            badRequest("Text value exceeds maximum length " + max);
        }
        return normalized;
    }

    private void badRequest(String message) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, message);
    }
}
