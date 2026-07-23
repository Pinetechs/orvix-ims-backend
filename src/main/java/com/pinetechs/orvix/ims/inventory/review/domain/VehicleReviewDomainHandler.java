package com.pinetechs.orvix.ims.inventory.review.domain;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.review.entity.InventoryRecheckItem;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckResult;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocation;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryScan;
import com.pinetechs.orvix.ims.inventory.vehicle.enums.VehicleInventoryItemStatus;
import com.pinetechs.orvix.ims.inventory.vehicle.enums.VehicleInventoryScanResult;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryScanRepository;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class VehicleReviewDomainHandler implements ReviewDomainHandler {

    private final VehicleInventoryItemRepository itemRepository;
    private final VehicleInventoryLocationRepository locationRepository;
    private final VehicleInventoryScanRepository scanRepository;
    private final InventoryTaskRepository taskRepository;

    public VehicleReviewDomainHandler(
            VehicleInventoryItemRepository itemRepository,
            VehicleInventoryLocationRepository locationRepository,
            VehicleInventoryScanRepository scanRepository,
            InventoryTaskRepository taskRepository
    ) {
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.scanRepository = scanRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public InventoryDomain domain() {
        return InventoryDomain.VEHICLE;
    }

    @Override
    public void validateSubmission(InventoryTask task, InventoryRecheckItem recheckItem) {
        if (!recheckItem.getResult().hasObservedResult()) return;
        if (recheckItem.getResult() != RecheckResult.FOUND_MATCHED
                && recheckItem.getResult() != RecheckResult.FOUND_DIFFERENT_LOCATION) {
            badRequest("Vehicle recheck result must be a vehicle result");
        }

        VehicleInventoryItem item = findItem(task.getId(), targetItemId(recheckItem));
        VehicleInventoryLocation location = findLocation(task.getId(), recheckItem.getLocationId());
        requireMatchingCode(recheckItem.getScannedCode(), item.getVinNo());

        boolean matched = equalsIgnoreCase(item.getStoreNo(), location.getStoreNo());
        RecheckResult expectedResult = matched
                ? RecheckResult.FOUND_MATCHED
                : RecheckResult.FOUND_DIFFERENT_LOCATION;
        if (recheckItem.getResult() != expectedResult) {
            badRequest("Vehicle recheck result does not match the selected location");
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
                    "A result without an observed vehicle cannot be accepted as the current result");
        }

        VehicleInventoryItem item = itemRepository
                .findForUpdateByTaskIdAndId(task.getId(), targetItemId(recheckItem))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Vehicle inventory item not found"));
        VehicleInventoryLocation actual = findLocation(task.getId(), recheckItem.getLocationId());

        VehicleInventoryScan previous = item.getCurrentScan();
        boolean wasProcessed = previous != null;
        boolean wasMatched = item.getStatus() == VehicleInventoryItemStatus.FOUND;
        boolean nowMatched = equalsIgnoreCase(item.getStoreNo(), actual.getStoreNo());

        VehicleInventoryScan scan = new VehicleInventoryScan();
        scan.setInventoryTask(task);
        scan.setItem(item);
        scan.setScannedBy(inventoryStaff);
        scan.setScannedVin(item.getVinNo());
        scan.setScanResult(nowMatched
                ? VehicleInventoryScanResult.FOUND
                : VehicleInventoryScanResult.WRONG_LOCATION);
        scan.setClientScanId(recheckItem.getClientSubmissionId());
        scan.setPayloadHash(recheckItem.getPayloadHash());
        scan.setEventType(InventoryScanEventType.RECHECK);
        scan.setCorrectsScan(previous);
        scan.setScanImage(recheckItem.getEvidenceImage());
        scan.setActualLocationEntity(actual);
        scan.setExpectedStoreNo(item.getStoreNo());
        scan.setExpectedLocation(item.getLocation());
        scan.setActualStoreNo(actual.getStoreNo());
        scan.setActualLocation(actual.getLocationName());
        scan.setDeviceScannedAt(recheckItem.getDeviceScannedAt());
        scan.setDeviceId(recheckItem.getDeviceId());
        scan.setSymbology(recheckItem.getSymbology());
        scan.setImageSource(recheckItem.getImageSource());
        scan.setNotes(recheckItem.getNote());
        scan = scanRepository.saveAndFlush(scan);

        item.setActualStoreNo(actual.getStoreNo());
        item.setActualLocation(actual.getLocationName());
        item.setStatus(nowMatched
                ? VehicleInventoryItemStatus.FOUND
                : VehicleInventoryItemStatus.MISMATCHED);
        item.setCheckedBy(inventoryStaff);
        item.setCheckedAt(LocalDateTime.now());
        item.setCurrentScan(scan);
        itemRepository.save(item);

        int processedDelta = wasProcessed ? 0 : 1;
        int matchedDelta = (nowMatched ? 1 : 0) - (wasMatched ? 1 : 0);
        taskRepository.adjustScanCounters(task.getId(), processedDelta, matchedDelta);
        locationRepository.findByInventoryTaskIdAndStoreNo(task.getId(), item.getStoreNo())
                .ifPresent(planned -> locationRepository.adjustScanCounters(
                        planned.getId(), processedDelta, matchedDelta));

        return new AppliedReviewResult(previous == null ? null : previous.getId(), scan.getId());
    }

    private VehicleInventoryItem findItem(Long taskId, Long itemId) {
        if (itemId == null) badRequest("resolvedItemId is required for this vehicle recheck");
        return itemRepository.findById(itemId)
                .filter(item -> taskId.equals(item.getInventoryTask().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Vehicle inventory item not found"));
    }

    private VehicleInventoryLocation findLocation(Long taskId, Long locationId) {
        if (locationId == null) badRequest("locationId is required for a found vehicle");
        return locationRepository.findById(locationId)
                .filter(location -> taskId.equals(location.getInventoryTask().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Vehicle inventory location not found"));
    }

    private Long targetItemId(InventoryRecheckItem item) {
        return item.getResolvedItemId() != null
                ? item.getResolvedItemId()
                : item.getReferenceItemId();
    }

    private void requireMatchingCode(String scannedCode, String expectedCode) {
        if (scannedCode == null || !scannedCode.trim().equalsIgnoreCase(expectedCode)) {
            badRequest("Scanned VIN does not match the recheck item");
        }
    }

    private boolean equalsIgnoreCase(String first, String second) {
        return first != null && second != null
                && first.trim().toUpperCase(Locale.ROOT)
                .equals(second.trim().toUpperCase(Locale.ROOT));
    }

    private void badRequest(String message) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, message);
    }
}
