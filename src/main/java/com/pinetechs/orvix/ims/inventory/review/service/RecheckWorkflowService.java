package com.pinetechs.orvix.ims.inventory.review.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.review.dto.*;
import com.pinetechs.orvix.ims.inventory.review.entity.*;
import com.pinetechs.orvix.ims.inventory.review.enums.*;
import com.pinetechs.orvix.ims.inventory.review.repository.*;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.enums.InventoryTaskActivityType;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskActivityService;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import com.pinetechs.orvix.ims.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class RecheckWorkflowService {

    private static final Set<RecheckItemStatus> ACTIVE_ITEM_STATUSES = EnumSet.of(
            RecheckItemStatus.PENDING,
            RecheckItemStatus.SUBMITTED
    );
    private static final DateTimeFormatter REQUEST_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final InventoryTaskRepository taskRepository;
    private final InventoryReviewIssueRepository issueRepository;
    private final InventoryRecheckRequestRepository requestRepository;
    private final InventoryRecheckItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ReviewResponseMapper mapper;
    private final RecheckItemContextService itemContextService;
    private final AccessPolicyService accessPolicyService;
    private final InventoryTaskActivityService taskActivityService;

    public RecheckWorkflowService(
            InventoryTaskRepository taskRepository,
            InventoryReviewIssueRepository issueRepository,
            InventoryRecheckRequestRepository requestRepository,
            InventoryRecheckItemRepository itemRepository,
            UserRepository userRepository,
            ReviewResponseMapper mapper,
            RecheckItemContextService itemContextService,
            AccessPolicyService accessPolicyService,
            InventoryTaskActivityService taskActivityService
    ) {
        this.taskRepository = taskRepository;
        this.issueRepository = issueRepository;
        this.requestRepository = requestRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.itemContextService = itemContextService;
        this.accessPolicyService = accessPolicyService;
        this.taskActivityService = taskActivityService;
    }

    @Transactional
    public RecheckRequestResponse create(
            Long taskId,
            CreateRecheckRequest input,
            User supervisor
    ) {
        InventoryTask task = requireTaskForUpdate(taskId);
        assertCanManage(task, supervisor);
        requireUnderReview(task);
        validateCreateInput(input);

        User assignedUser = requireEligibleInventoryStaff(
                input.getAssignedUserId(), task);
        List<InventoryReviewIssue> issues = requireIssues(
                taskId, input.getIssueIds());
        requireOneWorkArea(issues);
        requireIssuesAvailable(issues);

        InventoryRecheckRequest request = new InventoryRecheckRequest();
        request.setRequestNumber(generateRequestNumber(taskId));
        request.setInventoryTask(task);
        request.setInventoryDomain(task.getInventoryDomain());
        request.setAssignedTo(assignedUser);
        request.setRequestedBy(supervisor);
        request.setWorkAreaKey(issues.get(0).getWorkAreaKey());
        request.setWorkAreaLabel(issues.get(0).getWorkAreaLabel());
        request.setInstructions(trim(input.getInstructions(), 1500));
        request.setImageRequired(task.isScanImageRequired()
                || Boolean.TRUE.equals(input.getImageRequired()));
        request.setDueAt(input.getDueAt());

        groupIssues(issues).values().forEach(group -> {
            InventoryRecheckItem item = new InventoryRecheckItem();
            item.getIssues().addAll(group);
            item.setReferenceItemId(singleReferenceItemId(group));
            item.setPreviousScanId(firstCurrentScanId(group));
            itemContextService.applyExpectedContext(task, item);
            request.addItem(item);
            group.forEach(InventoryReviewIssue::markRecheckRequested);
        });

        InventoryRecheckRequest saved = requestRepository.save(request);
        issueRepository.saveAll(issues);
        taskActivityService.record(
                task,
                InventoryTaskActivityType.RECHECK_REQUESTED,
                task.getStatus(),
                task.getStatus(),
                supervisor,
                null,
                "Recheck " + saved.getRequestNumber()
                        + " assigned to " + assignedUser.getUsername()
                        + " with " + saved.getItems().size() + " item(s)"
        );
        return mapper.recheckRequest(saved);
    }

    @Transactional(readOnly = true)
    public Page<RecheckRequestResponse> appRequests(
            RecheckRequestStatus status,
            Pageable pageable,
            User user
    ) {
        assertCanUseRechecks(user);
        Collection<RecheckRequestStatus> statuses = status == null
                ? ReviewCenterService.ACTIVE_RECHECK_STATUSES
                : List.of(status);
        return requestRepository.findByAssignedToIdAndStatusIn(
                        user.getId(), statuses, pageable)
                .map(mapper::recheckRequest);
    }

    @Transactional(readOnly = true)
    public RecheckRequestResponse appRequest(Long requestId, User user) {
        assertCanUseRechecks(user);
        InventoryRecheckRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Recheck request not found"));
        assertAssignedTo(request, user);
        return mapper.recheckRequest(request);
    }

    @Transactional(readOnly = true)
    public RecheckRequestResponse supervisorRequest(
            Long taskId,
            Long requestId,
            User user
    ) {
        InventoryRecheckRequest request = requestRepository.findById(requestId)
                .filter(value -> taskId.equals(value.getInventoryTask().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Recheck request not found"));
        accessPolicyService.assertCanViewInventoryTask(user, request.getInventoryTask());
        return mapper.recheckRequest(request);
    }

    @Transactional
    public RecheckRequestResponse start(Long requestId, User user) {
        assertCanUseRechecks(user);
        InventoryRecheckRequest request = requireRequestForUpdate(requestId);
        assertAssignedTo(request, user);
        requireUnderReview(request.getInventoryTask());

        if (request.getStatus() == RecheckRequestStatus.IN_PROGRESS) {
            return mapper.recheckRequest(request);
        }
        if (request.getStatus() != RecheckRequestStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Only a pending recheck request can be started");
        }

        LocalDateTime now = LocalDateTime.now();
        request.start(now);
        request.getItems().stream()
                .flatMap(item -> item.getIssues().stream())
                .filter(issue -> issue.getStatus().isOpen())
                .forEach(InventoryReviewIssue::markRecheckInProgress);
        requestRepository.save(request);
        taskActivityService.record(
                request.getInventoryTask(),
                InventoryTaskActivityType.RECHECK_STARTED,
                request.getInventoryTask().getStatus(),
                request.getInventoryTask().getStatus(),
                user,
                null,
                "Recheck " + request.getRequestNumber() + " started"
        );
        return mapper.recheckRequest(request);
    }

    @Transactional
    public RecheckRequestResponse cancel(
            Long taskId,
            Long requestId,
            String reason,
            User supervisor
    ) {
        InventoryRecheckRequest request = requireRequestForUpdate(requestId);
        if (!taskId.equals(request.getInventoryTask().getId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "Recheck request not found");
        }
        assertCanManage(request.getInventoryTask(), supervisor);
        requireUnderReview(request.getInventoryTask());
        if (!request.getStatus().isActive()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Recheck request is already closed");
        }
        String normalizedReason = requireText(reason, 500,
                "Cancellation reason is required");

        request.getItems().forEach(item -> {
            if (!item.getStatus().isFinal()) item.cancel();
            item.getIssues().stream()
                    .filter(issue -> issue.getStatus().isOpen())
                    .forEach(InventoryReviewIssue::markOpen);
        });
        request.cancel(normalizedReason, LocalDateTime.now());
        requestRepository.save(request);
        return mapper.recheckRequest(request);
    }

    @Transactional(readOnly = true)
    public UploadedFile supervisorEvidence(
            Long taskId,
            Long requestId,
            Long itemId,
            User user
    ) {
        InventoryRecheckItem item = itemRepository.findById(itemId)
                .filter(value -> requestId.equals(value.getRecheckRequest().getId()))
                .filter(value -> taskId.equals(
                        value.getRecheckRequest().getInventoryTask().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Recheck evidence not found"));
        accessPolicyService.assertCanViewInventoryTask(
                user, item.getRecheckRequest().getInventoryTask());
        return requireEvidence(item);
    }

    @Transactional(readOnly = true)
    public UploadedFile appEvidence(
            Long requestId,
            Long itemId,
            User user
    ) {
        assertCanUseRechecks(user);
        InventoryRecheckItem item = itemRepository.findById(itemId)
                .filter(value -> requestId.equals(value.getRecheckRequest().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Recheck evidence not found"));
        assertAssignedTo(item.getRecheckRequest(), user);
        return requireEvidence(item);
    }

    private Map<String, List<InventoryReviewIssue>> groupIssues(
            List<InventoryReviewIssue> issues
    ) {
        Map<String, List<InventoryReviewIssue>> groups = new LinkedHashMap<>();
        for (InventoryReviewIssue issue : issues) {
            String key = issue.getItemId() != null
                    ? "ITEM:" + issue.getItemId()
                    : "SCAN:" + issue.getSourceScanId();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(issue);
        }
        return groups;
    }

    private List<InventoryReviewIssue> requireIssues(
            Long taskId,
            List<Long> issueIds
    ) {
        List<Long> distinctIds = issueIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            badRequest("At least one valid review issue is required");
        }
        List<InventoryReviewIssue> issues = issueRepository
                .findAllForUpdate(taskId, distinctIds);
        if (issues.size() != distinctIds.size()) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "One or more review issues were not found");
        }
        issues.sort(Comparator.comparing(InventoryReviewIssue::getId));
        return issues;
    }

    private void requireIssuesAvailable(List<InventoryReviewIssue> issues) {
        for (InventoryReviewIssue issue : issues) {
            if (issue.getStatus() != ReviewIssueStatus.OPEN) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "Review issue " + issue.getId() + " is not open");
            }
            if (itemRepository.existsByIssueIdAndStatusIn(
                    issue.getId(), ACTIVE_ITEM_STATUSES)) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "Review issue " + issue.getId()
                                + " already belongs to an active recheck");
            }
        }
    }

    private void requireOneWorkArea(List<InventoryReviewIssue> issues) {
        Set<String> workAreas = new HashSet<>();
        for (InventoryReviewIssue issue : issues) {
            workAreas.add(issue.getWorkAreaKey());
        }
        if (workAreas.size() != 1 || workAreas.contains(null)) {
            badRequest("A recheck request must contain issues from one work area");
        }
    }

    private Long singleReferenceItemId(List<InventoryReviewIssue> issues) {
        return issues.stream()
                .map(InventoryReviewIssue::getItemId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Long firstCurrentScanId(List<InventoryReviewIssue> issues) {
        return issues.stream()
                .map(InventoryReviewIssue::getCurrentScanId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private User requireEligibleInventoryStaff(Long userId, InventoryTask task) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Assigned inventory staff user not found"));
        boolean appAccess = user.getAccessChannel() == AccessChannel.APP
                || user.getAccessChannel() == AccessChannel.BOTH;
        if (!user.isInventoryStaff()
                || !Boolean.TRUE.equals(user.getEnabled())
                || Boolean.TRUE.equals(user.getDeleted())
                || !appAccess
                || !user.hasCompany(task.getCompany().getId())
                || !user.hasInventoryDomain(task.getInventoryDomain())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Selected user is not eligible for this recheck request");
        }
        return user;
    }

    private InventoryTask requireTaskForUpdate(Long taskId) {
        return taskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Inventory task not found"));
    }

    private InventoryRecheckRequest requireRequestForUpdate(Long requestId) {
        return requestRepository.findForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Recheck request not found"));
    }

    private UploadedFile requireEvidence(InventoryRecheckItem item) {
        UploadedFile file = item.getEvidenceImage();
        if (file == null
                || Boolean.TRUE.equals(file.getDeleted())
                || Boolean.TRUE.equals(file.getTemp())) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "Recheck evidence not found");
        }
        return file;
    }

    private void assertAssignedTo(InventoryRecheckRequest request, User user) {
        if (user == null || user.getId() == null
                || request.getAssignedTo() == null
                || !user.getId().equals(request.getAssignedTo().getId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "Assigned recheck request not found");
        }
    }

    private void assertCanUseRechecks(User user) {
        accessPolicyService.assertCanUseApp(user);
    }

    private void assertCanManage(InventoryTask task, User user) {
        accessPolicyService.assertCanUpdateTask(
                user, task.getCompany().getId(), task.getInventoryDomain());
    }

    private void requireUnderReview(InventoryTask task) {
        if (task.getStatus() != InventoryTaskStatus.UNDER_REVIEW) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Recheck is available only while the task is UNDER_REVIEW");
        }
    }

    private void validateCreateInput(CreateRecheckRequest input) {
        if (input == null) badRequest("Recheck request is required");
        if (input.getAssignedUserId() == null) badRequest("assignedUserId is required");
        if (input.getIssueIds() == null || input.getIssueIds().isEmpty()) {
            badRequest("At least one review issue is required");
        }
        if (input.getDueAt() != null
                && !input.getDueAt().isAfter(LocalDateTime.now())) {
            badRequest("dueAt must be in the future");
        }
    }

    private String generateRequestNumber(Long taskId) {
        String suffix = UUID.randomUUID().toString()
                .substring(0, 4).toUpperCase(Locale.ROOT);
        return "RCHK-" + taskId + "-"
                + LocalDateTime.now().format(REQUEST_TIME)
                + "-" + suffix;
    }

    private String requireText(String value, int max, String message) {
        String normalized = trim(value, max);
        if (normalized == null) badRequest(message);
        return normalized;
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
