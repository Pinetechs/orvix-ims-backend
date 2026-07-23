package com.pinetechs.orvix.ims.inventory.review.controller;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.inventory.review.dto.*;
import com.pinetechs.orvix.ims.inventory.review.enums.*;
import com.pinetechs.orvix.ims.inventory.review.service.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/tasks/{taskId}/review")
public class InventoryReviewController {

    private static final java.util.Set<String> ISSUE_SORT_FIELDS = java.util.Set.of(
            "id", "detectedAt", "issueType", "status", "itemCode", "workAreaLabel");

    private final ReviewCenterService reviewCenterService;
    private final RecheckWorkflowService recheckWorkflowService;
    private final ReviewDecisionService decisionService;
    private final Helper helper;

    public InventoryReviewController(
            ReviewCenterService reviewCenterService,
            RecheckWorkflowService recheckWorkflowService,
            ReviewDecisionService decisionService,
            Helper helper
    ) {
        this.reviewCenterService = reviewCenterService;
        this.recheckWorkflowService = recheckWorkflowService;
        this.decisionService = decisionService;
        this.helper = helper;
    }

    @GetMapping("/summary")
    public ReviewCenterSummaryResponse summary(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return reviewCenterService.summary(
                taskId, helper.currentUser(authentication));
    }

    @PostMapping("/synchronize")
    public ReviewCenterSummaryResponse synchronize(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return reviewCenterService.synchronize(
                taskId, helper.currentUser(authentication));
    }

    @GetMapping("/issues")
    public Page<ReviewIssueResponse> issues(
            @PathVariable Long taskId,
            @RequestParam(required = false) ReviewIssueStatus status,
            @RequestParam(required = false) ReviewIssueType type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "detectedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            Authentication authentication
    ) {
        if (!ISSUE_SORT_FIELDS.contains(sortBy)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Unsupported review issue sort field");
        }
        Sort sort = "asc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return reviewCenterService.issues(
                taskId,
                status,
                type,
                search,
                PageRequest.of(page, size, sort),
                helper.currentUser(authentication)
        );
    }

    @GetMapping("/issues/{issueId}")
    public ReviewIssueDetailsResponse issue(
            @PathVariable Long taskId,
            @PathVariable Long issueId,
            Authentication authentication
    ) {
        return reviewCenterService.issueDetails(
                taskId, issueId, helper.currentUser(authentication));
    }

    @PostMapping("/issues/{issueId}/decision")
    public ReviewIssueDetailsResponse decideIssue(
            @PathVariable Long taskId,
            @PathVariable Long issueId,
            @RequestBody ReviewDecisionRequest request,
            Authentication authentication
    ) {
        return decisionService.decideIssue(
                taskId,
                issueId,
                request,
                helper.currentUser(authentication)
        );
    }

    @PostMapping("/rechecks")
    public RecheckRequestResponse createRecheck(
            @PathVariable Long taskId,
            @RequestBody CreateRecheckRequest request,
            Authentication authentication
    ) {
        return recheckWorkflowService.create(
                taskId, request, helper.currentUser(authentication));
    }

    @GetMapping("/rechecks")
    public Page<RecheckRequestResponse> rechecks(
            @PathVariable Long taskId,
            @RequestParam(required = false) RecheckRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return reviewCenterService.recheckRequests(
                taskId,
                status,
                PageRequest.of(page, size, Sort.by("createdAt").descending()),
                helper.currentUser(authentication)
        );
    }

    @GetMapping("/rechecks/{requestId}")
    public RecheckRequestResponse recheck(
            @PathVariable Long taskId,
            @PathVariable Long requestId,
            Authentication authentication
    ) {
        return recheckWorkflowService.supervisorRequest(
                taskId, requestId, helper.currentUser(authentication));
    }

    @PostMapping("/rechecks/{requestId}/cancel")
    public RecheckRequestResponse cancelRecheck(
            @PathVariable Long taskId,
            @PathVariable Long requestId,
            @RequestBody CancelRecheckRequest request,
            Authentication authentication
    ) {
        return recheckWorkflowService.cancel(
                taskId,
                requestId,
                request == null ? null : request.getReason(),
                helper.currentUser(authentication)
        );
    }

    @PostMapping("/rechecks/{requestId}/items/{itemId}/decision")
    public RecheckRequestResponse decideRecheckItem(
            @PathVariable Long taskId,
            @PathVariable Long requestId,
            @PathVariable Long itemId,
            @RequestBody ReviewDecisionRequest request,
            Authentication authentication
    ) {
        return decisionService.decideRecheckItem(
                taskId,
                requestId,
                itemId,
                request,
                helper.currentUser(authentication)
        );
    }

    @GetMapping("/rechecks/{requestId}/items/{itemId}/evidence")
    public ResponseEntity<Resource> evidence(
            @PathVariable Long taskId,
            @PathVariable Long requestId,
            @PathVariable Long itemId,
            Authentication authentication
    ) {
        UploadedFile file = recheckWorkflowService.supervisorEvidence(
                taskId,
                requestId,
                itemId,
                helper.currentUser(authentication)
        );
        return imageResponse(file);
    }

    private ResponseEntity<Resource> imageResponse(UploadedFile file) {
        Resource resource = new FileSystemResource(file.getFilePath());
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Recheck evidence content not found");
        }
        MediaType contentType;
        try {
            contentType = file.getContentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(file.getContentType());
        } catch (IllegalArgumentException ex) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"recheck-evidence\"")
                .body(resource);
    }
}
