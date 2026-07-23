package com.pinetechs.orvix.ims.inventory.review.service;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.review.entity.InventoryReviewIssue;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewIssueStatus;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewIssueType;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewDecisionType;
import com.pinetechs.orvix.ims.inventory.review.repository.InventoryReviewIssueRepository;
import com.pinetechs.orvix.ims.inventory.review.repository.InventoryReviewDecisionRepository;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingResultFilter;
import com.pinetechs.orvix.ims.inventory.tracking.provider.InventoryTrackingProvider;
import com.pinetechs.orvix.ims.inventory.tracking.provider.InventoryTrackingProviderRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReviewIssueSynchronizationService {

    private static final int PAGE_SIZE = 500;
    private static final Set<ReviewIssueStatus> ACTIVE_STATUSES = EnumSet.of(
            ReviewIssueStatus.OPEN,
            ReviewIssueStatus.RECHECK_REQUESTED,
            ReviewIssueStatus.RECHECK_IN_PROGRESS,
            ReviewIssueStatus.RECHECK_SUBMITTED
    );

    private final InventoryReviewIssueRepository issueRepository;
    private final InventoryReviewDecisionRepository decisionRepository;
    private final InventoryTrackingProviderRegistry providerRegistry;

    public ReviewIssueSynchronizationService(
            InventoryReviewIssueRepository issueRepository,
            InventoryReviewDecisionRepository decisionRepository,
            InventoryTrackingProviderRegistry providerRegistry
    ) {
        this.issueRepository = issueRepository;
        this.decisionRepository = decisionRepository;
        this.providerRegistry = providerRegistry;
    }

    @Transactional
    public long synchronize(InventoryTask task) {
        InventoryTrackingProvider provider = providerRegistry.get(task.getInventoryDomain());
        Set<Long> approvedScanIds = new HashSet<>(
                decisionRepository.findAcceptedScanIdsByTaskId(
                        task.getId(), ReviewDecisionType.ACCEPT_RECHECK_RESULT));
        Map<String, ReviewIssueCandidate> currentCandidates =
                detectCandidates(task, provider, approvedScanIds);
        List<InventoryReviewIssue> existing = issueRepository
                .findByInventoryTaskIdAndStatusIn(task.getId(), ACTIVE_STATUSES);
        Map<String, InventoryReviewIssue> existingCandidates =
                loadExistingCandidates(task.getId(), currentCandidates.keySet());
        Map<String, InventoryReviewIssue> activeByKey = new HashMap<>();
        for (InventoryReviewIssue issue : existing) {
            activeByKey.put(issue.getIssueKey(), issue);
        }

        List<InventoryReviewIssue> changed = new ArrayList<>();
        for (ReviewIssueCandidate candidate : currentCandidates.values()) {
            InventoryReviewIssue issue = existingCandidates.get(candidate.issueKey());
            if (issue == null) issue = newIssue(task, candidate);

            // A final supervisor decision stays final for the same evidence.
            // A later scan produces a different issue key and therefore a new issue.
            if (issue.getStatus() == ReviewIssueStatus.RESOLVED
                    || issue.getStatus() == ReviewIssueStatus.SUPERSEDED) {
                activeByKey.remove(candidate.issueKey());
                continue;
            }

            copyCandidate(issue, candidate);
            changed.add(issue);
            activeByKey.remove(candidate.issueKey());
        }

        LocalDateTime now = LocalDateTime.now();
        for (InventoryReviewIssue stale : activeByKey.values()) {
            if (stale.getStatus() == ReviewIssueStatus.OPEN) {
                stale.supersede(now);
                changed.add(stale);
            }
        }

        issueRepository.saveAll(changed);
        return issueRepository.countByInventoryTaskIdAndStatusIn(task.getId(), ACTIVE_STATUSES);
    }

    private Map<String, InventoryReviewIssue> loadExistingCandidates(
            Long taskId,
            Set<String> issueKeys
    ) {
        Map<String, InventoryReviewIssue> result = new HashMap<>();
        List<String> keys = new ArrayList<>(issueKeys);
        for (int start = 0; start < keys.size(); start += PAGE_SIZE) {
            int end = Math.min(start + PAGE_SIZE, keys.size());
            issueRepository.findByInventoryTaskIdAndIssueKeyIn(
                            taskId, keys.subList(start, end))
                    .forEach(issue -> result.put(issue.getIssueKey(), issue));
        }
        return result;
    }

    private Map<String, ReviewIssueCandidate> detectCandidates(
            InventoryTask task,
            InventoryTrackingProvider provider,
            Set<Long> approvedScanIds
    ) {
        Map<String, ReviewIssueCandidate> result = new LinkedHashMap<>();

        readAllResults(provider, task.getId(), TrackingResultFilter.REMAINING)
                .forEach(item -> addResultCandidate(result, item, ReviewIssueType.NOT_PROCESSED));

        readAllResults(provider, task.getId(), TrackingResultFilter.MISMATCHED)
                .stream()
                .filter(item -> item.currentScanId() == null
                        || !approvedScanIds.contains(item.currentScanId()))
                .forEach(item -> issueTypes(item).forEach(type ->
                        addResultCandidate(result, item, type)));

        addEventCandidates(result, provider, task.getId(),
                InventoryScanEventType.EXTRA, ReviewIssueType.EXTRA);
        addEventCandidates(result, provider, task.getId(),
                InventoryScanEventType.AMBIGUOUS, ReviewIssueType.AMBIGUOUS);
        addEventCandidates(result, provider, task.getId(),
                InventoryScanEventType.CONFLICT, ReviewIssueType.CONFLICT);
        return result;
    }

    private List<TrackingResponses.ResultItem> readAllResults(
            InventoryTrackingProvider provider,
            Long taskId,
            TrackingResultFilter filter
    ) {
        List<TrackingResponses.ResultItem> result = new ArrayList<>();
        int pageNumber = 0;
        Page<TrackingResponses.ResultItem> page;
        do {
            page = provider.results(
                    taskId,
                    filter,
                    null,
                    PageRequest.of(pageNumber++, PAGE_SIZE)
            );
            result.addAll(page.getContent());
        } while (page.hasNext());
        return result;
    }

    private void addEventCandidates(
            Map<String, ReviewIssueCandidate> candidates,
            InventoryTrackingProvider provider,
            Long taskId,
            InventoryScanEventType eventType,
            ReviewIssueType issueType
    ) {
        int pageNumber = 0;
        Page<TrackingResponses.ScanEvent> page;
        do {
            page = provider.unresolvedScanEvents(
                    taskId,
                    eventType,
                    PageRequest.of(pageNumber++, PAGE_SIZE)
            );
            for (TrackingResponses.ScanEvent event : page.getContent()) {
                ReviewIssueCandidate candidate = fromEvent(event, issueType);
                candidates.put(candidate.issueKey(), candidate);
            }
        } while (page.hasNext());
    }

    private void addResultCandidate(
            Map<String, ReviewIssueCandidate> candidates,
            TrackingResponses.ResultItem item,
            ReviewIssueType issueType
    ) {
        ReviewIssueCandidate candidate = fromResult(item, issueType);
        candidates.put(candidate.issueKey(), candidate);
    }

    private Set<ReviewIssueType> issueTypes(TrackingResponses.ResultItem item) {
        String status = item.status() == null
                ? ""
                : item.status().trim().toUpperCase(Locale.ROOT);
        Set<ReviewIssueType> types = EnumSet.noneOf(ReviewIssueType.class);

        if (item.domain() == InventoryDomain.VEHICLE) {
            if ("MISMATCHED".equals(status)) types.add(ReviewIssueType.LOCATION_MISMATCH);
            if ("MISSING".equals(status)) types.add(ReviewIssueType.NOT_PROCESSED);
            return types;
        }

        if (item.domain() == InventoryDomain.ASSET) {
            if ("LOCATION_MISMATCH".equals(status)) types.add(ReviewIssueType.LOCATION_MISMATCH);
            if ("DUPLICATE_REVIEW".equals(status)) types.add(ReviewIssueType.CONFLICT);
            if ("EXTRA".equals(status)) types.add(ReviewIssueType.EXTRA);
            return types;
        }

        if (status.contains("LOCATION_MISMATCH")) {
            types.add(ReviewIssueType.LOCATION_MISMATCH);
        }
        if (status.contains("SHORTAGE")) {
            types.add(ReviewIssueType.QUANTITY_SHORTAGE);
        }
        if (status.contains("OVERAGE")) {
            types.add(ReviewIssueType.QUANTITY_OVERAGE);
        }
        if ("REVIEW_REQUIRED".equals(status)) {
            types.add(ReviewIssueType.CONFLICT);
        }
        if ("EXTRA".equals(status)) {
            types.add(ReviewIssueType.EXTRA);
        }
        return types;
    }

    private ReviewIssueCandidate fromResult(
            TrackingResponses.ResultItem item,
            ReviewIssueType type
    ) {
        String issueKey = "ITEM:" + item.itemId()
                + ":TYPE:" + type
                + ":SCAN:" + value(item.currentScanId());
        String workArea = firstNotBlank(item.expectedArea(), item.actualArea(), "UNASSIGNED");
        return new ReviewIssueCandidate(
                issueKey,
                item.domain(),
                type,
                item.itemId(),
                null,
                item.currentScanId(),
                item.code(),
                item.secondaryCode(),
                item.description(),
                areaKey(workArea),
                workArea,
                item.expectedArea(),
                item.expectedSubArea(),
                item.expectedLeafArea(),
                item.actualArea(),
                item.actualSubArea(),
                item.actualLeafArea(),
                item.expectedQuantity(),
                item.actualQuantity(),
                item.varianceQuantity(),
                item.acceptedAt() == null ? LocalDateTime.now() : item.acceptedAt()
        );
    }

    private ReviewIssueCandidate fromEvent(
            TrackingResponses.ScanEvent event,
            ReviewIssueType type
    ) {
        String workArea = firstNotBlank(event.actualArea(), event.expectedArea(), "UNASSIGNED");
        return new ReviewIssueCandidate(
                "SCAN:" + event.scanId() + ":TYPE:" + type,
                event.domain(),
                type,
                event.itemId(),
                event.scanId(),
                null,
                event.scannedCode(),
                null,
                event.details(),
                areaKey(workArea),
                workArea,
                event.expectedArea(),
                event.expectedSubArea(),
                event.expectedLeafArea(),
                event.actualArea(),
                event.actualSubArea(),
                event.actualLeafArea(),
                event.expectedQuantity(),
                event.actualQuantity(),
                event.varianceQuantity(),
                event.scannedAt() == null ? LocalDateTime.now() : event.scannedAt()
        );
    }

    private InventoryReviewIssue newIssue(
            InventoryTask task,
            ReviewIssueCandidate candidate
    ) {
        InventoryReviewIssue issue = new InventoryReviewIssue();
        issue.setInventoryTask(task);
        issue.setInventoryDomain(candidate.domain());
        issue.setIssueKey(candidate.issueKey());
        issue.setIssueType(candidate.issueType());
        issue.setStatus(ReviewIssueStatus.OPEN);
        issue.setBlocking(true);
        return issue;
    }

    private void copyCandidate(
            InventoryReviewIssue issue,
            ReviewIssueCandidate candidate
    ) {
        issue.setItemId(candidate.itemId());
        issue.setSourceScanId(candidate.sourceScanId());
        issue.setCurrentScanId(candidate.currentScanId());
        issue.setItemCode(candidate.itemCode());
        issue.setSecondaryCode(candidate.secondaryCode());
        issue.setItemDescription(candidate.itemDescription());
        issue.setWorkAreaKey(candidate.workAreaKey());
        issue.setWorkAreaLabel(candidate.workAreaLabel());
        issue.setExpectedArea(candidate.expectedArea());
        issue.setExpectedSubArea(candidate.expectedSubArea());
        issue.setExpectedLeafArea(candidate.expectedLeafArea());
        issue.setActualArea(candidate.actualArea());
        issue.setActualSubArea(candidate.actualSubArea());
        issue.setActualLeafArea(candidate.actualLeafArea());
        issue.setExpectedQuantity(candidate.expectedQuantity());
        issue.setActualQuantity(candidate.actualQuantity());
        issue.setVarianceQuantity(candidate.varianceQuantity());
        issue.setDetectedAt(candidate.detectedAt());
    }

    private String areaKey(String value) {
        return value.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "UNASSIGNED";
    }

    private String value(Long value) {
        return value == null ? "NONE" : value.toString();
    }
}
