package com.pinetechs.orvix.ims.app.service;

import com.pinetechs.orvix.ims.app.dto.AppScanCorrectionRequest;
import com.pinetechs.orvix.ims.app.dto.AppScanItemSummaryResponse;
import com.pinetechs.orvix.ims.app.dto.AppScanLocationSummaryResponse;
import com.pinetechs.orvix.ims.app.dto.AppScanRequest;
import com.pinetechs.orvix.ims.app.dto.AppScanResponse;
import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.file.service.UploadedFileService;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.sparepart.entity.*;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.*;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.*;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskActivityService;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppSparePartScanService {

    private final AppScanSupport support;
    private final SparePartInventoryItemRepository itemRepository;
    private final SparePartInventoryBranchRepository branchRepository;
    private final SparePartInventoryLocationRepository locationRepository;
    private final SparePartInventoryBranchAssignmentRepository assignmentRepository;
    private final SparePartInventoryScanRepository scanRepository;
    private final InventoryTaskRepository taskRepository;
    private final InventoryTaskActivityService taskActivityService;
    private final UploadedFileService uploadedFileService;

    public AppSparePartScanService(
            AppScanSupport support,
            SparePartInventoryItemRepository itemRepository,
            SparePartInventoryBranchRepository branchRepository,
            SparePartInventoryLocationRepository locationRepository,
            SparePartInventoryBranchAssignmentRepository assignmentRepository,
            SparePartInventoryScanRepository scanRepository,
            InventoryTaskRepository taskRepository,
            InventoryTaskActivityService taskActivityService,
            UploadedFileService uploadedFileService
    ) {
        this.support = support;
        this.itemRepository = itemRepository;
        this.branchRepository = branchRepository;
        this.locationRepository = locationRepository;
        this.assignmentRepository = assignmentRepository;
        this.scanRepository = scanRepository;
        this.taskRepository = taskRepository;
        this.taskActivityService = taskActivityService;
        this.uploadedFileService = uploadedFileService;
    }

    @Transactional(readOnly = true)
    public Optional<AppScanResponse> replay(Long taskId, String clientScanId, String payloadHash) {
        return scanRepository.findByInventoryTaskIdAndClientScanId(taskId, clientScanId)
                .map(scan -> replayResponse(scan, payloadHash));
    }

    @Transactional
    public AppScanResponse scan(Long taskId, AppScanRequest request, User user, UploadedFile image, String payloadHash) {
        InventoryTask task = support.requireAssignedScannableTask(taskId, user, InventoryDomain.SPARE_PART);
        support.requireQuantityPermission(user);
        String itemNo = support.requireCode(request.getCode());
        BigDecimal countedQty = requireQuantity(request.getCountedQty());
        SparePath actual = requireAssignedPath(taskId, request.getBranchId(), request.getLocationId(), user);

        SparePartInventoryItem item = itemRepository.findExactForUpdate(taskId, itemNo, actual.branch().getId(), actual.location().getId()).orElse(null);
        List<SparePartInventoryItem> candidates = List.of();
        if (item == null) {
            candidates = itemRepository.findCandidatesForUpdate(taskId, itemNo);
            if (candidates.size() == 1) item = candidates.get(0);
        }

        SparePartInventoryScan scan = baseScan(task, item, user, itemNo, request, attach(image), payloadHash, actual, countedQty);
        if (item == null) {
            boolean ambiguous = candidates.size() > 1;
            scan.setEventType(ambiguous ? InventoryScanEventType.AMBIGUOUS : InventoryScanEventType.EXTRA);
            scan.setScanResult(ambiguous ? SparePartInventoryScanResult.AMBIGUOUS : SparePartInventoryScanResult.EXTRA);
            scan.setLocationStatus(ambiguous
                    ? SparePartInventoryLocationStatus.AMBIGUOUS
                    : SparePartInventoryLocationStatus.EXTRA_ITEM);
            scan.setQuantityStatus(SparePartInventoryQuantityStatus.NOT_APPLICABLE);
            scan.setMessage(ambiguous ? "AMBIGUOUS_ITEM_NUMBER" : "ITEM_NOT_IN_TASK");
            scan.setReviewRequired(true);
            scan = scanRepository.saveAndFlush(scan);
            reopenLocation(taskId, actual.location());
            startTask(task, user);
            return response(scan, null, "REVIEW", "scan.recorded_for_review", true, false);
        }

        Outcome outcome = outcome(item, actual, countedQty);
        if (item.getCurrentScan() != null || item.getStatus() != SparePartInventoryItemStatus.NOT_COUNTED) {
            boolean sameLocation = sameCanonicalLocation(item, actual);
            boolean sameQuantity = item.getActualQty() != null && item.getActualQty().compareTo(countedQty) == 0;
            boolean duplicate = sameLocation && sameQuantity;
            scan.setEventType(duplicate ? InventoryScanEventType.DUPLICATE : InventoryScanEventType.CONFLICT);
            scan.setScanResult(!sameLocation
                    ? SparePartInventoryScanResult.RECOUNT_DIFFERENT_LOCATION
                    : sameQuantity
                        ? SparePartInventoryScanResult.RECOUNT_SAME_LOCATION
                        : SparePartInventoryScanResult.RECOUNT_DIFFERENT_QUANTITY);
            applyInternalOutcome(scan, item, outcome);
            scan.setMessage(duplicate ? "RECOUNT_DUPLICATE" : "RECOUNT_REQUIRES_CORRECTION");
            scan.setReviewRequired(!duplicate);
            scan = scanRepository.saveAndFlush(scan);
            reopenLocation(taskId, actual.location());
            return response(scan, item, "ALREADY_COUNTED", "scan.already_counted", true, true);
        }

        scan.setEventType(InventoryScanEventType.FIRST_SCAN);
        applyInternalOutcome(scan, item, outcome);
        scan.setReviewRequired(outcome.itemStatus() != SparePartInventoryItemStatus.MATCHED);
        scan = scanRepository.saveAndFlush(scan);
        applyCanonicalItem(item, actual, countedQty, outcome, user, scan);
        itemRepository.save(item);
        adjustCounters(item, null, outcome, 1);
        reopenLocation(taskId, actual.location());
        startTask(task, user);
        return response(scan, item, "RECORDED", "scan.recorded", true, outcome.locationMismatch());
    }

    @Transactional
    public AppScanResponse correct(Long taskId, Long currentScanId, AppScanCorrectionRequest request,
                                   User user, UploadedFile image, String payloadHash) {
        InventoryTask task = support.requireAssignedScannableTask(taskId, user, InventoryDomain.SPARE_PART);
        support.requireQuantityPermission(user);
        support.requireCorrectionPermission(user);
        String reason = support.trim(request.getReason(), 1000);
        if (reason == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "Correction reason is required");
        BigDecimal countedQty = requireQuantity(request.getCountedQty());

        SparePartInventoryScan currentScan = scanRepository.findByIdAndInventoryTaskId(currentScanId, taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Spare part scan not found"));
        if (currentScan.getItem() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "An extra or ambiguous scan cannot become an item correction");
        }
        SparePartInventoryItem item = itemRepository
                .findForUpdateByTaskIdAndId(taskId, currentScan.getItem().getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Spare part inventory item not found"));
        requireCurrentOwnedScan(item, currentScan, currentScanId, user);
        SparePath actual = requireAssignedPath(taskId, request.getBranchId(), request.getLocationId(), user);

        Outcome previous = outcomeFromItem(item);
        Outcome corrected = outcome(item, actual, countedQty);
        AppScanRequest scanRequest = correctionAsScanRequest(request, item.getItemNo());
        SparePartInventoryScan correction = baseScan(task, item, user, item.getItemNo(), scanRequest,
                attach(image), payloadHash, actual, countedQty);
        correction.setEventType(InventoryScanEventType.CORRECTION);
        correction.setCorrectsScan(currentScan);
        correction.setNotes(reason);
        applyInternalOutcome(correction, item, corrected);
        correction.setScanResult(SparePartInventoryScanResult.CORRECTED);
        correction.setMessage("CORRECTION_RECORDED");
        correction.setReviewRequired(corrected.itemStatus() != SparePartInventoryItemStatus.MATCHED);
        scanRepository.resolveOpenItemReviews(
                taskId, item.getId(), LocalDateTime.now(), user);
        correction = scanRepository.saveAndFlush(correction);

        applyCanonicalItem(item, actual, countedQty, corrected, user, correction);
        itemRepository.save(item);
        adjustCounters(item, previous, corrected, 0);
        reopenLocation(taskId, actual.location());
        return response(correction, item, "CORRECTED", "scan.correction_recorded", true, false);
    }

    private SparePath requireAssignedPath(Long taskId, Long branchId, Long locationId, User user) {
        if (branchId == null || locationId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "branchId and locationId are required for spare part scan");
        }
        SparePartInventoryBranch branch = branchRepository.findById(branchId)
                .filter(value -> taskId.equals(value.getInventoryTask().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Spare part branch not found"));
        if (!assignmentRepository.existsActiveByTaskIdAndUserIdAndBranchId(taskId, user.getId(), branchId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Spare part branch is not assigned to the current user");
        }
        SparePartInventoryLocation location = locationRepository
                .findForUpdate(taskId, branchId, locationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Spare part location does not belong to the selected branch"));
        return new SparePath(branch, location);
    }

    private BigDecimal requireQuantity(BigDecimal quantity) {
        if (quantity == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "countedQty is required");
        if (quantity.signum() < 0) throw new BusinessException(HttpStatus.BAD_REQUEST, "countedQty cannot be negative");
        if (quantity.scale() > 3) throw new BusinessException(HttpStatus.BAD_REQUEST, "countedQty supports at most 3 decimal places");
        if (quantity.precision() > 18) throw new BusinessException(HttpStatus.BAD_REQUEST, "countedQty exceeds maximum precision");
        return quantity;
    }


    // احتساب
    private Outcome outcome(SparePartInventoryItem item, SparePath actual, BigDecimal countedQty) {
        boolean locationMatched = item.getPlannedBranch().getId().equals(actual.branch().getId()) && item.getPlannedLocation().getId().equals(actual.location().getId());
        BigDecimal stockQty = item.getStockQty();
        if (stockQty == null) throw new BusinessException(HttpStatus.CONFLICT, "Spare part stock quantity is not configured");
        int comparison = countedQty.compareTo(stockQty);
        SparePartInventoryQuantityStatus quantityStatus = comparison == 0
                ? SparePartInventoryQuantityStatus.MATCHED_QTY
                : comparison < 0 ? SparePartInventoryQuantityStatus.SHORTAGE_QTY
                : SparePartInventoryQuantityStatus.OVERAGE_QTY;
        SparePartInventoryLocationStatus locationStatus = locationMatched
                ? SparePartInventoryLocationStatus.CORRECT_LOCATION
                : SparePartInventoryLocationStatus.WRONG_LOCATION;
        SparePartInventoryItemStatus itemStatus = itemStatus(locationMatched, comparison);
        SparePartInventoryScanResult scanResult = scanResult(locationMatched, comparison);
        return new Outcome(locationStatus, quantityStatus, itemStatus, scanResult, countedQty.subtract(stockQty));
    }

    private Outcome outcomeFromItem(SparePartInventoryItem item) {
        boolean locationMatched = item.getPlannedBranch().getId().equals(item.getActualBranch().getId())
                && item.getPlannedLocation().getId().equals(item.getActualLocation().getId());
        int comparison = item.getActualQty().compareTo(item.getStockQty());
        return new Outcome(
                locationMatched ? SparePartInventoryLocationStatus.CORRECT_LOCATION : SparePartInventoryLocationStatus.WRONG_LOCATION,
                comparison == 0 ? SparePartInventoryQuantityStatus.MATCHED_QTY
                        : comparison < 0 ? SparePartInventoryQuantityStatus.SHORTAGE_QTY : SparePartInventoryQuantityStatus.OVERAGE_QTY,
                item.getStatus(), scanResult(locationMatched, comparison), item.getVarianceQty());
    }

    private SparePartInventoryItemStatus itemStatus(boolean locationMatched, int comparison) {
        if (locationMatched) {
            if (comparison == 0) return SparePartInventoryItemStatus.MATCHED;
            return comparison < 0 ? SparePartInventoryItemStatus.SHORTAGE : SparePartInventoryItemStatus.OVERAGE;
        }
        if (comparison == 0) return SparePartInventoryItemStatus.LOCATION_MISMATCH;
        return comparison < 0 ? SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_SHORTAGE
                : SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_OVERAGE;
    }

    private SparePartInventoryScanResult scanResult(boolean locationMatched, int comparison) {
        if (locationMatched) {
            if (comparison == 0) return SparePartInventoryScanResult.MATCHED;
            return comparison < 0 ? SparePartInventoryScanResult.SHORTAGE : SparePartInventoryScanResult.OVERAGE;
        }
        if (comparison == 0) return SparePartInventoryScanResult.LOCATION_MISMATCH;
        return comparison < 0 ? SparePartInventoryScanResult.LOCATION_MISMATCH_WITH_SHORTAGE
                : SparePartInventoryScanResult.LOCATION_MISMATCH_WITH_OVERAGE;
    }

    private SparePartInventoryScan baseScan(InventoryTask task, SparePartInventoryItem item, User user,
                                            String itemNo, AppScanRequest request, UploadedFile image,
                                            String payloadHash, SparePath actual, BigDecimal countedQty) {
        SparePartInventoryScan scan = new SparePartInventoryScan();
        scan.setInventoryTask(task);
        scan.setItem(item);
        scan.setScannedItemNo(itemNo);
        scan.setScannedBy(user);
        scan.setClientScanId(request.getClientScanId().trim());
        scan.setPayloadHash(payloadHash);
        scan.setScanImage(image);
        scan.setActualBranch(actual.branch());
        scan.setActualLocation(actual.location());
        scan.setCountedQty(countedQty);
        scan.setDeviceScannedAt(request.getDeviceScannedAt());
        scan.setDeviceId(support.trim(request.getDeviceId(), 150));
        scan.setSymbology(support.trim(request.getSymbology(), 80));
        scan.setImageSource(support.imageSource(request.getImageSource()));
        scan.setNotes(support.trim(request.getNotes(), 1000));
        return scan;
    }

    private void applyInternalOutcome(SparePartInventoryScan scan, SparePartInventoryItem item, Outcome outcome) {
        scan.setExpectedBranch(item.getPlannedBranch());
        scan.setExpectedLocation(item.getPlannedLocation());
        scan.setLocationStatus(outcome.locationStatus());
        scan.setQuantityStatus(outcome.quantityStatus());
        scan.setScanResult(outcome.scanResult());
        scan.setStockQty(item.getStockQty());
        scan.setVarianceQty(outcome.variance());
        scan.setMessage(outcome.itemStatus().name());
    }

    private void applyCanonicalItem(SparePartInventoryItem item, SparePath actual, BigDecimal countedQty,
                                    Outcome outcome, User user, SparePartInventoryScan scan) {
        item.setActualBranch(actual.branch());
        item.setActualLocation(actual.location());
        item.setActualQty(countedQty);
        item.setVarianceQty(outcome.variance());
        item.setStatus(outcome.itemStatus());
        item.setCountedBy(user);
        item.setCountedAt(LocalDateTime.now());
        item.setCurrentScan(scan);
    }

    private void adjustCounters(SparePartInventoryItem item, Outcome previous, Outcome current, int countedDelta) {
        CounterDelta previousCounters = CounterDelta.from(previous);
        CounterDelta currentCounters = CounterDelta.from(current);
        int matchedDelta = currentCounters.matched() - previousCounters.matched();
        int shortageDelta = currentCounters.shortage() - previousCounters.shortage();
        int overageDelta = currentCounters.overage() - previousCounters.overage();
        int locationDelta = currentCounters.locationMismatch() - previousCounters.locationMismatch();
        taskRepository.adjustScanCounters(item.getInventoryTask().getId(), countedDelta, matchedDelta);
        branchRepository.adjustScanCounters(item.getPlannedBranch().getId(), countedDelta,
                matchedDelta, shortageDelta, overageDelta, locationDelta);
        locationRepository.adjustScanCounters(item.getPlannedLocation().getId(), countedDelta,
                matchedDelta, shortageDelta, overageDelta, locationDelta);
    }

    private boolean sameCanonicalLocation(SparePartInventoryItem item, SparePath actual) {
        return item.getActualBranch() != null && item.getActualLocation() != null
                && item.getActualBranch().getId().equals(actual.branch().getId())
                && item.getActualLocation().getId().equals(actual.location().getId());
    }

    private void requireCurrentOwnedScan(SparePartInventoryItem item, SparePartInventoryScan scan, Long scanId, User user) {
        if (item.getCurrentScan() == null || !scanId.equals(item.getCurrentScan().getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "The scan is no longer the current accepted scan");
        }
        if (scan.getScannedBy() == null || !user.getId().equals(scan.getScannedBy().getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Inventory staff can correct only their own scan");
        }
    }

    private UploadedFile attach(UploadedFile image) {
        return image == null ? null : uploadedFileService.markAsAttached(image.getId());
    }

    private void startTask(InventoryTask task, User user) {
        taskActivityService.startOnFirstScan(task, user);
    }

    private void reopenLocation(Long taskId, SparePartInventoryLocation location) {
        if (location == null) return;
        SparePartInventoryLocation lockedLocation = locationRepository.findForUpdate(
                        taskId, location.getBranch().getId(), location.getId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT, "Spare part location changed during scan"));
        if (!lockedLocation.isCompleted()) return;
        lockedLocation.setCompleted(false);
        lockedLocation.setCompletedAt(null);
        lockedLocation.setCompletedBy(null);
    }

    private AppScanResponse replayResponse(SparePartInventoryScan scan, String payloadHash) {
        if (!scan.getPayloadHash().equals(payloadHash)) {
            throw new BusinessException(HttpStatus.CONFLICT, "clientScanId was already used with different scan data");
        }
        String result = switch (scan.getEventType()) {
            case EXTRA, AMBIGUOUS -> "REVIEW";
            case DUPLICATE, CONFLICT -> "ALREADY_COUNTED";
            case CORRECTION -> "CORRECTED";
            default -> "RECORDED";
        };
        String message = switch (scan.getEventType()) {
            case EXTRA, AMBIGUOUS -> "scan.recorded_for_review";
            case DUPLICATE, CONFLICT -> "scan.already_counted";
            case CORRECTION -> "scan.correction_recorded";
            default -> "scan.recorded";
        };
        boolean correctionAllowed = scan.getEventType() == InventoryScanEventType.CONFLICT
                || scan.getEventType() == InventoryScanEventType.DUPLICATE
                || (scan.getEventType() == InventoryScanEventType.FIRST_SCAN
                    && scan.getLocationStatus() == SparePartInventoryLocationStatus.WRONG_LOCATION);
        AppScanResponse response = response(scan, scan.getItem(), result, message, true, correctionAllowed);
        response.setIdempotentReplay(true);
        return response;
    }

    /** App response deliberately omits stock, variance and expected location. */
    private AppScanResponse response(SparePartInventoryScan scan, SparePartInventoryItem item, String result,
                                     String messageKey, boolean accepted, boolean correctionAllowed) {
        AppScanResponse response = new AppScanResponse();
        response.setScanId(scan.getId());
        response.setItemId(item == null ? null : item.getId());
        response.setCurrentAcceptedScanId(item == null || item.getCurrentScan() == null
                ? null : item.getCurrentScan().getId());
        response.setImageFileId(scan.getScanImage() == null ? null : scan.getScanImage().getId());
        response.setInventoryDomain(InventoryDomain.SPARE_PART);
        response.setClientScanId(scan.getClientScanId());
        response.setEventType(scan.getEventType().name());
        response.setResultCode(result);
        response.setMessageKey(messageKey);
        response.setAccepted(accepted);
        response.setCorrectionAllowed(correctionAllowed);
        response.setServerScannedAt(scan.getScannedAt());
        response.setItem(itemSummary(scan, item));
        response.setActualLocation(actualLocationSummary(scan));
        return response;
    }

    private AppScanItemSummaryResponse itemSummary(
            SparePartInventoryScan scan,
            SparePartInventoryItem item
    ) {
        AppScanItemSummaryResponse summary = new AppScanItemSummaryResponse();
        String itemNo = item == null ? scan.getScannedItemNo() : item.getItemNo();
        summary.setCode(itemNo);
        summary.setBarcode(itemNo);
        summary.setDisplayName(item == null || item.getBrandName() == null
                ? itemNo
                : item.getBrandName() + " " + itemNo);
        summary.setCategory("SPARE_PART");
        summary.setBrand(item == null ? null : item.getBrandName());
        summary.setCountedQuantity(scan.getCountedQty());
        return summary;
    }

    private AppScanLocationSummaryResponse actualLocationSummary(SparePartInventoryScan scan) {
        AppScanLocationSummaryResponse summary = new AppScanLocationSummaryResponse();
        if (scan.getActualBranch() != null) {
            summary.setBranchId(scan.getActualBranch().getId());
            summary.setBranchName(scan.getActualBranch().getBranchName());
        }
        if (scan.getActualLocation() != null) {
            summary.setLocationId(scan.getActualLocation().getId());
            summary.setLocationCode(scan.getActualLocation().getLocationCode());
            summary.setLocationName(scan.getActualLocation().getLocationCode());
        }
        return summary;
    }

    private AppScanRequest correctionAsScanRequest(AppScanCorrectionRequest correction, String code) {
        AppScanRequest request = new AppScanRequest();
        request.setClientScanId(correction.getClientScanId());
        request.setCode(code);
        request.setBranchId(correction.getBranchId());
        request.setLocationId(correction.getLocationId());
        request.setCountedQty(correction.getCountedQty());
        request.setDeviceScannedAt(correction.getDeviceScannedAt());
        request.setDeviceId(correction.getDeviceId());
        request.setSymbology(correction.getSymbology());
        request.setImageSource(correction.getImageSource());
        return request;
    }

    private record SparePath(SparePartInventoryBranch branch, SparePartInventoryLocation location) {}
    private record Outcome(SparePartInventoryLocationStatus locationStatus,
                           SparePartInventoryQuantityStatus quantityStatus,
                           SparePartInventoryItemStatus itemStatus,
                           SparePartInventoryScanResult scanResult,
                           BigDecimal variance) {
        boolean locationMismatch() { return locationStatus == SparePartInventoryLocationStatus.WRONG_LOCATION; }
    }
    private record CounterDelta(int matched, int shortage, int overage, int locationMismatch) {
        static CounterDelta from(Outcome outcome) {
            if (outcome == null) return new CounterDelta(0, 0, 0, 0);
            boolean shortage = outcome.quantityStatus() == SparePartInventoryQuantityStatus.SHORTAGE_QTY;
            boolean overage = outcome.quantityStatus() == SparePartInventoryQuantityStatus.OVERAGE_QTY;
            return new CounterDelta(
                    outcome.itemStatus() == SparePartInventoryItemStatus.MATCHED ? 1 : 0,
                    shortage ? 1 : 0,
                    overage ? 1 : 0,
                    outcome.locationMismatch() ? 1 : 0);
        }
    }
}
