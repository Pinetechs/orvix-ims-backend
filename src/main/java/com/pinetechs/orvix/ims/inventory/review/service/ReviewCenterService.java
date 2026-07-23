package com.pinetechs.orvix.ims.inventory.review.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.review.dto.*;
import com.pinetechs.orvix.ims.inventory.review.entity.InventoryReviewIssue;
import com.pinetechs.orvix.ims.inventory.review.enums.*;
import com.pinetechs.orvix.ims.inventory.review.repository.*;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReviewCenterService {

    public static final Set<ReviewIssueStatus> OPEN_ISSUE_STATUSES =
            Collections.unmodifiableSet(EnumSet.of(
                    ReviewIssueStatus.OPEN,
                    ReviewIssueStatus.RECHECK_REQUESTED,
                    ReviewIssueStatus.RECHECK_IN_PROGRESS,
                    ReviewIssueStatus.RECHECK_SUBMITTED
            ));
    public static final Set<RecheckRequestStatus> ACTIVE_RECHECK_STATUSES =
            Collections.unmodifiableSet(EnumSet.of(
                    RecheckRequestStatus.PENDING,
                    RecheckRequestStatus.IN_PROGRESS,
                    RecheckRequestStatus.SUBMITTED
            ));

    private final InventoryTaskRepository taskRepository;
    private final InventoryReviewIssueRepository issueRepository;
    private final InventoryReviewDecisionRepository decisionRepository;
    private final InventoryRecheckRequestRepository recheckRequestRepository;
    private final ReviewIssueSynchronizationService synchronizationService;
    private final ReviewResponseMapper mapper;
    private final AccessPolicyService accessPolicyService;

    public ReviewCenterService(
            InventoryTaskRepository taskRepository,
            InventoryReviewIssueRepository issueRepository,
            InventoryReviewDecisionRepository decisionRepository,
            InventoryRecheckRequestRepository recheckRequestRepository,
            ReviewIssueSynchronizationService synchronizationService,
            ReviewResponseMapper mapper,
            AccessPolicyService accessPolicyService
    ) {
        this.taskRepository = taskRepository;
        this.issueRepository = issueRepository;
        this.decisionRepository = decisionRepository;
        this.recheckRequestRepository = recheckRequestRepository;
        this.synchronizationService = synchronizationService;
        this.mapper = mapper;
        this.accessPolicyService = accessPolicyService;
    }

    @Transactional(readOnly = true)
    public ReviewCenterSummaryResponse summary(Long taskId, User user) {
        InventoryTask task = requireViewableTask(taskId, user);
        return buildSummary(task);
    }

    @Transactional
    public ReviewCenterSummaryResponse synchronize(Long taskId, User user) {
        InventoryTask task = requireTask(taskId);
        assertCanManage(task, user);
        requireUnderReview(task);
        synchronizationService.synchronize(task);
        return buildSummary(task);
    }

    @Transactional(readOnly = true)
    public Page<ReviewIssueResponse> issues(
            Long taskId,
            ReviewIssueStatus status,
            ReviewIssueType type,
            String search,
            Pageable pageable,
            User user
    ) {
        requireViewableTask(taskId, user);
        String normalizedSearch = normalize(search);
        Specification<InventoryReviewIssue> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("inventoryTask").get("id"), taskId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (type != null) predicates.add(cb.equal(root.get("issueType"), type));
            if (normalizedSearch != null) {
                String like = "%" + normalizedSearch.toUpperCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.upper(root.get("itemCode")), like),
                        cb.like(cb.upper(root.get("secondaryCode")), like),
                        cb.like(cb.upper(root.get("itemDescription")), like),
                        cb.like(cb.upper(root.get("workAreaLabel")), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return issueRepository.findAll(specification, pageable).map(mapper::issue);
    }

    @Transactional(readOnly = true)
    public ReviewIssueDetailsResponse issueDetails(Long taskId, Long issueId, User user) {
        requireViewableTask(taskId, user);
        InventoryReviewIssue issue = issueRepository.findById(issueId)
                .filter(value -> taskId.equals(value.getInventoryTask().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Review issue not found"));
        List<ReviewDecisionResponse> decisions = decisionRepository.findByIssueId(issueId)
                .stream()
                .map(mapper::decision)
                .toList();
        return new ReviewIssueDetailsResponse(mapper.issue(issue), decisions);
    }

    @Transactional(readOnly = true)
    public Page<RecheckRequestResponse> recheckRequests(
            Long taskId,
            RecheckRequestStatus status,
            Pageable pageable,
            User user
    ) {
        requireViewableTask(taskId, user);
        Page<com.pinetechs.orvix.ims.inventory.review.entity.InventoryRecheckRequest> page =
                status == null
                        ? recheckRequestRepository.findByInventoryTaskId(taskId, pageable)
                        : recheckRequestRepository.findByInventoryTaskIdAndStatus(
                                taskId, status, pageable);
        return page.map(mapper::recheckRequest);
    }

    public long countBlockingOpenIssues(Long taskId) {
        return issueRepository.countByInventoryTaskIdAndBlockingTrueAndStatusIn(
                taskId, OPEN_ISSUE_STATUSES);
    }

    public long countActiveRechecks(Long taskId) {
        return recheckRequestRepository.countByInventoryTaskIdAndStatusIn(
                taskId, ACTIVE_RECHECK_STATUSES);
    }

    private ReviewCenterSummaryResponse buildSummary(InventoryTask task) {
        Map<ReviewIssueStatus, Long> byStatus = new EnumMap<>(ReviewIssueStatus.class);
        for (ReviewIssueStatus value : ReviewIssueStatus.values()) byStatus.put(value, 0L);
        issueRepository.countByStatus(task.getId())
                .forEach(row -> byStatus.put(
                        (ReviewIssueStatus) row[0],
                        ((Number) row[1]).longValue()));

        Map<ReviewIssueType, Long> byType = new EnumMap<>(ReviewIssueType.class);
        for (ReviewIssueType value : ReviewIssueType.values()) byType.put(value, 0L);
        issueRepository.countOpenByType(
                        task.getId(),
                        List.of(ReviewIssueStatus.RESOLVED, ReviewIssueStatus.SUPERSEDED))
                .forEach(row -> byType.put(
                        (ReviewIssueType) row[0],
                        ((Number) row[1]).longValue()));

        long blocking = countBlockingOpenIssues(task.getId());
        long activeRechecks = countActiveRechecks(task.getId());
        return new ReviewCenterSummaryResponse(
                task.getId(),
                task.getTaskNumber(),
                task.getInventoryDomain(),
                Collections.unmodifiableMap(byStatus),
                Collections.unmodifiableMap(byType),
                blocking,
                activeRechecks,
                task.getStatus() == InventoryTaskStatus.UNDER_REVIEW
                        && blocking == 0
                        && activeRechecks == 0
        );
    }

    private InventoryTask requireViewableTask(Long taskId, User user) {
        InventoryTask task = requireTask(taskId);
        accessPolicyService.assertCanViewInventoryTask(user, task);
        return task;
    }

    private InventoryTask requireTask(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task ID is required");
        }
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Inventory task not found"));
    }

    private void assertCanManage(InventoryTask task, User user) {
        accessPolicyService.assertCanUpdateTask(
                user, task.getCompany().getId(), task.getInventoryDomain());
    }

    private void requireUnderReview(InventoryTask task) {
        if (task.getStatus() != InventoryTaskStatus.UNDER_REVIEW) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Review Center changes are allowed only while the task is UNDER_REVIEW");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
