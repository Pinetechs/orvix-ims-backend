package com.pinetechs.orvix.ims.inventory.review.domain;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.review.entity.InventoryRecheckItem;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckResult;
import com.pinetechs.orvix.ims.inventory.sparepart.entity.*;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.*;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.*;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class SparePartReviewDomainHandler implements ReviewDomainHandler {

    private final SparePartInventoryItemRepository itemRepository;
    private final SparePartInventoryBranchRepository branchRepository;
    private final SparePartInventoryLocationRepository locationRepository;
    private final SparePartInventoryScanRepository scanRepository;
    private final InventoryTaskRepository taskRepository;

    public SparePartReviewDomainHandler(
            SparePartInventoryItemRepository itemRepository,
            SparePartInventoryBranchRepository branchRepository,
            SparePartInventoryLocationRepository locationRepository,
            SparePartInventoryScanRepository scanRepository,
            InventoryTaskRepository taskRepository
    ) {
        this.itemRepository = itemRepository;
        this.branchRepository = branchRepository;
        this.locationRepository = locationRepository;
        this.scanRepository = scanRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public InventoryDomain domain() {
        return InventoryDomain.SPARE_PART;
    }

    @Override
    public void validateSubmission(InventoryTask task, InventoryRecheckItem recheckItem) {
        if (!recheckItem.getResult().hasObservedResult()) return;
        if (recheckItem.getResult() != RecheckResult.QUANTITY_CONFIRMED
                && recheckItem.getResult() != RecheckResult.QUANTITY_DIFFERENT) {
            badRequest("Spare-part recheck result must be a quantity result");
        }

        SparePartInventoryItem item = findItem(task.getId(), targetItemId(recheckItem));
        if (item.getStockQty() == null) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Spare-part stock quantity is not configured");
        }
        findPath(task.getId(), recheckItem);
        requireMatchingCode(recheckItem.getScannedCode(), item.getItemNo());
        requireQuantity(recheckItem.getCountedQuantity());

        RecheckResult expectedResult =
                item.getStockQty().compareTo(recheckItem.getCountedQuantity()) == 0
                ? RecheckResult.QUANTITY_CONFIRMED
                : RecheckResult.QUANTITY_DIFFERENT;
        if (recheckItem.getResult() != expectedResult) {
            badRequest("Spare-part recheck result does not match the submitted quantity");
        }
        recheckItem.setResolvedItemId(item.getId());
    }

    @Override
    public AppliedReviewResult accept(
            InventoryTask task,
            InventoryRecheckItem recheckItem,
            User inventoryStaff
    ) {
        if (!recheckItem.getResult().hasObservedResult()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "A result without an observed quantity cannot be accepted as the current result");
        }

        SparePartInventoryItem item = itemRepository
                .findForUpdateByTaskIdAndId(task.getId(), targetItemId(recheckItem))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Spare-part inventory item not found"));
        SparePath actual = findPath(task.getId(), recheckItem);
        BigDecimal countedQuantity = requireQuantity(recheckItem.getCountedQuantity());
        Outcome outcome = outcome(item, actual, countedQuantity);

        SparePartInventoryScan previous = item.getCurrentScan();
        CounterFlags oldFlags = CounterFlags.from(item.getStatus(), previous != null);
        CounterFlags newFlags = CounterFlags.from(outcome.itemStatus(), true);

        SparePartInventoryScan scan = new SparePartInventoryScan();
        scan.setInventoryTask(task);
        scan.setItem(item);
        scan.setScannedItemNo(item.getItemNo());
        scan.setScannedBy(inventoryStaff);
        scan.setExpectedBranch(item.getPlannedBranch());
        scan.setExpectedLocation(item.getPlannedLocation());
        scan.setActualBranch(actual.branch());
        scan.setActualLocation(actual.location());
        scan.setLocationStatus(outcome.locationStatus());
        scan.setQuantityStatus(outcome.quantityStatus());
        scan.setScanResult(outcome.scanResult());
        scan.setStockQty(item.getStockQty());
        scan.setCountedQty(countedQuantity);
        scan.setVarianceQty(outcome.variance());
        scan.setMessage(outcome.itemStatus().name());
        scan.setClientScanId(recheckItem.getClientSubmissionId());
        scan.setPayloadHash(recheckItem.getPayloadHash());
        scan.setEventType(InventoryScanEventType.RECHECK);
        scan.setCorrectsScan(previous);
        scan.setScanImage(recheckItem.getEvidenceImage());
        scan.setDeviceScannedAt(recheckItem.getDeviceScannedAt());
        scan.setDeviceId(recheckItem.getDeviceId());
        scan.setSymbology(recheckItem.getSymbology());
        scan.setImageSource(recheckItem.getImageSource());
        scan.setNotes(recheckItem.getNote());
        scan.setReviewRequired(false);
        scan = scanRepository.saveAndFlush(scan);

        item.setActualBranch(actual.branch());
        item.setActualLocation(actual.location());
        item.setActualQty(countedQuantity);
        item.setVarianceQty(outcome.variance());
        item.setStatus(outcome.itemStatus());
        item.setCountedBy(inventoryStaff);
        item.setCountedAt(LocalDateTime.now());
        item.setCurrentScan(scan);
        itemRepository.save(item);

        int countedDelta = newFlags.counted() - oldFlags.counted();
        int matchedDelta = newFlags.matched() - oldFlags.matched();
        int shortageDelta = newFlags.shortage() - oldFlags.shortage();
        int overageDelta = newFlags.overage() - oldFlags.overage();
        int locationDelta = newFlags.locationMismatch() - oldFlags.locationMismatch();

        taskRepository.adjustScanCounters(task.getId(), countedDelta, matchedDelta);
        branchRepository.adjustScanCounters(
                item.getPlannedBranch().getId(),
                countedDelta, matchedDelta, shortageDelta, overageDelta, locationDelta);
        locationRepository.adjustScanCounters(
                item.getPlannedLocation().getId(),
                countedDelta, matchedDelta, shortageDelta, overageDelta, locationDelta);

        return new AppliedReviewResult(previous == null ? null : previous.getId(), scan.getId());
    }

    private SparePartInventoryItem findItem(Long taskId, Long itemId) {
        if (itemId == null) badRequest("resolvedItemId is required for this spare-part recheck");
        return itemRepository.findById(itemId)
                .filter(item -> taskId.equals(item.getInventoryTask().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Spare-part inventory item not found"));
    }

    private SparePath findPath(Long taskId, InventoryRecheckItem recheckItem) {
        Long branchId = recheckItem.getBranchId();
        Long locationId = recheckItem.getLocationId();
        if (branchId == null || locationId == null) {
            badRequest("branchId and locationId are required for a counted spare part");
        }

        SparePartInventoryBranch branch = branchRepository.findById(branchId)
                .filter(value -> taskId.equals(value.getInventoryTask().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Spare-part branch not found"));
        SparePartInventoryLocation location = locationRepository
                .findForUpdate(taskId, branchId, locationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "Spare-part location does not belong to the selected branch"));
        return new SparePath(branch, location);
    }

    private Outcome outcome(
            SparePartInventoryItem item,
            SparePath actual,
            BigDecimal countedQuantity
    ) {
        if (item.getStockQty() == null) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Spare-part stock quantity is not configured");
        }
        boolean locationMatched = item.getPlannedBranch().getId().equals(actual.branch().getId())
                && item.getPlannedLocation().getId().equals(actual.location().getId());
        int comparison = countedQuantity.compareTo(item.getStockQty());

        SparePartInventoryLocationStatus locationStatus = locationMatched
                ? SparePartInventoryLocationStatus.CORRECT_LOCATION
                : SparePartInventoryLocationStatus.WRONG_LOCATION;
        SparePartInventoryQuantityStatus quantityStatus = comparison == 0
                ? SparePartInventoryQuantityStatus.MATCHED_QTY
                : comparison < 0
                    ? SparePartInventoryQuantityStatus.SHORTAGE_QTY
                    : SparePartInventoryQuantityStatus.OVERAGE_QTY;
        SparePartInventoryItemStatus itemStatus = itemStatus(locationMatched, comparison);
        SparePartInventoryScanResult scanResult = scanResult(locationMatched, comparison);
        return new Outcome(
                locationStatus,
                quantityStatus,
                itemStatus,
                scanResult,
                countedQuantity.subtract(item.getStockQty())
        );
    }

    private SparePartInventoryItemStatus itemStatus(boolean locationMatched, int comparison) {
        if (locationMatched) {
            if (comparison == 0) return SparePartInventoryItemStatus.MATCHED;
            return comparison < 0
                    ? SparePartInventoryItemStatus.SHORTAGE
                    : SparePartInventoryItemStatus.OVERAGE;
        }
        if (comparison == 0) return SparePartInventoryItemStatus.LOCATION_MISMATCH;
        return comparison < 0
                ? SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_SHORTAGE
                : SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_OVERAGE;
    }

    private SparePartInventoryScanResult scanResult(boolean locationMatched, int comparison) {
        if (locationMatched) {
            if (comparison == 0) return SparePartInventoryScanResult.MATCHED;
            return comparison < 0
                    ? SparePartInventoryScanResult.SHORTAGE
                    : SparePartInventoryScanResult.OVERAGE;
        }
        if (comparison == 0) return SparePartInventoryScanResult.LOCATION_MISMATCH;
        return comparison < 0
                ? SparePartInventoryScanResult.LOCATION_MISMATCH_WITH_SHORTAGE
                : SparePartInventoryScanResult.LOCATION_MISMATCH_WITH_OVERAGE;
    }

    private BigDecimal requireQuantity(BigDecimal quantity) {
        if (quantity == null) badRequest("countedQuantity is required");
        if (quantity.signum() < 0) badRequest("countedQuantity cannot be negative");
        if (quantity.scale() > 3) badRequest("countedQuantity supports at most 3 decimal places");
        if (quantity.precision() > 18) badRequest("countedQuantity exceeds maximum precision");
        return quantity;
    }

    private Long targetItemId(InventoryRecheckItem item) {
        return item.getResolvedItemId() != null
                ? item.getResolvedItemId()
                : item.getReferenceItemId();
    }

    private void requireMatchingCode(String scannedCode, String expectedCode) {
        if (scannedCode == null || !scannedCode.trim().equalsIgnoreCase(expectedCode)) {
            badRequest("Scanned item number does not match the recheck item");
        }
    }

    private void badRequest(String message) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    private record SparePath(
            SparePartInventoryBranch branch,
            SparePartInventoryLocation location
    ) {}

    private record Outcome(
            SparePartInventoryLocationStatus locationStatus,
            SparePartInventoryQuantityStatus quantityStatus,
            SparePartInventoryItemStatus itemStatus,
            SparePartInventoryScanResult scanResult,
            BigDecimal variance
    ) {}

    private record CounterFlags(
            int counted,
            int matched,
            int shortage,
            int overage,
            int locationMismatch
    ) {
        private static CounterFlags from(SparePartInventoryItemStatus status, boolean counted) {
            if (!counted || status == null || status == SparePartInventoryItemStatus.NOT_COUNTED) {
                return new CounterFlags(0, 0, 0, 0, 0);
            }
            int matched = status == SparePartInventoryItemStatus.MATCHED ? 1 : 0;
            int shortage = status == SparePartInventoryItemStatus.SHORTAGE
                    || status == SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_SHORTAGE ? 1 : 0;
            int overage = status == SparePartInventoryItemStatus.OVERAGE
                    || status == SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_OVERAGE ? 1 : 0;
            int locationMismatch = status == SparePartInventoryItemStatus.LOCATION_MISMATCH
                    || status == SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_SHORTAGE
                    || status == SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_OVERAGE ? 1 : 0;
            return new CounterFlags(1, matched, shortage, overage, locationMismatch);
        }
    }
}
