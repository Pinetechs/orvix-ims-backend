package com.pinetechs.orvix.ims.inventory.review.domain;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.asset.entity.*;
import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryItemStatus;
import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryScanResult;
import com.pinetechs.orvix.ims.inventory.asset.repository.*;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.review.entity.InventoryRecheckItem;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckResult;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class AssetReviewDomainHandler implements ReviewDomainHandler {

    private final AssetInventoryItemRepository itemRepository;
    private final AssetInventoryLocationRepository locationRepository;
    private final AssetInventoryFloorRepository floorRepository;
    private final AssetInventoryPlaceRepository placeRepository;
    private final AssetInventoryScanRepository scanRepository;
    private final InventoryTaskRepository taskRepository;

    public AssetReviewDomainHandler(
            AssetInventoryItemRepository itemRepository,
            AssetInventoryLocationRepository locationRepository,
            AssetInventoryFloorRepository floorRepository,
            AssetInventoryPlaceRepository placeRepository,
            AssetInventoryScanRepository scanRepository,
            InventoryTaskRepository taskRepository
    ) {
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.floorRepository = floorRepository;
        this.placeRepository = placeRepository;
        this.scanRepository = scanRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public InventoryDomain domain() {
        return InventoryDomain.ASSET;
    }

    @Override
    public void validateSubmission(InventoryTask task, InventoryRecheckItem recheckItem) {
        if (!recheckItem.getResult().hasObservedResult()) return;
        if (recheckItem.getResult() != RecheckResult.FOUND_MATCHED
                && recheckItem.getResult() != RecheckResult.FOUND_DIFFERENT_LOCATION) {
            badRequest("Asset recheck result must be an asset result");
        }

        AssetInventoryItem item = findItem(task.getId(), targetItemId(recheckItem));
        AssetPath actual = findPath(task.getId(), recheckItem);
        requireMatchingCode(recheckItem.getScannedCode(), item.getBarcode());

        boolean matched = mismatchFields(item, actual).isEmpty();
        RecheckResult expectedResult = matched
                ? RecheckResult.FOUND_MATCHED
                : RecheckResult.FOUND_DIFFERENT_LOCATION;
        if (recheckItem.getResult() != expectedResult) {
            badRequest("Asset recheck result does not match the selected location");
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
                    "A result without an observed asset cannot be accepted as the current result");
        }

        AssetInventoryItem item = itemRepository
                .findForUpdateByTaskIdAndId(task.getId(), targetItemId(recheckItem))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Asset inventory item not found"));
        AssetPath actual = findPath(task.getId(), recheckItem);
        List<String> mismatches = mismatchFields(item, actual);

        AssetInventoryScan previous = item.getCurrentScan();
        boolean wasProcessed = previous != null;
        boolean wasMatched = item.getStatus() == AssetInventoryItemStatus.MATCHED;
        boolean nowMatched = mismatches.isEmpty();

        AssetInventoryScan scan = new AssetInventoryScan();
        scan.setInventoryTask(task);
        scan.setItem(item);
        scan.setScannedBy(inventoryStaff);
        scan.setScannedBarcode(item.getBarcode());
        scan.setScanResult(nowMatched
                ? AssetInventoryScanResult.MATCHED
                : AssetInventoryScanResult.LOCATION_MISMATCH);
        scan.setClientScanId(recheckItem.getClientSubmissionId());
        scan.setPayloadHash(recheckItem.getPayloadHash());
        scan.setEventType(InventoryScanEventType.RECHECK);
        scan.setCorrectsScan(previous);
        scan.setScanImage(recheckItem.getEvidenceImage());
        scan.setExpectedLocation(item.getPlannedLocation());
        scan.setExpectedFloor(item.getPlannedFloor());
        scan.setExpectedPlace(item.getPlannedPlace());
        scan.setActualLocation(actual.location());
        scan.setActualFloor(actual.floor());
        scan.setActualPlace(actual.place());
        scan.setMismatchFields(String.join(",", mismatches));
        scan.setDeviceScannedAt(recheckItem.getDeviceScannedAt());
        scan.setDeviceId(recheckItem.getDeviceId());
        scan.setSymbology(recheckItem.getSymbology());
        scan.setImageSource(recheckItem.getImageSource());
        scan.setNotes(recheckItem.getNote());
        scan = scanRepository.saveAndFlush(scan);

        item.setActualLocation(actual.location());
        item.setActualFloor(actual.floor());
        item.setActualPlace(actual.place());
        item.setStatus(nowMatched
                ? AssetInventoryItemStatus.MATCHED
                : AssetInventoryItemStatus.LOCATION_MISMATCH);
        item.setCheckedBy(inventoryStaff);
        item.setCheckedAt(LocalDateTime.now());
        item.setCurrentScan(scan);
        itemRepository.save(item);

        int processedDelta = wasProcessed ? 0 : 1;
        int matchedDelta = (nowMatched ? 1 : 0) - (wasMatched ? 1 : 0);
        taskRepository.adjustScanCounters(task.getId(), processedDelta, matchedDelta);
        locationRepository.adjustScanCounters(
                item.getPlannedLocation().getId(), processedDelta, matchedDelta);

        return new AppliedReviewResult(previous == null ? null : previous.getId(), scan.getId());
    }

    private AssetInventoryItem findItem(Long taskId, Long itemId) {
        if (itemId == null) badRequest("resolvedItemId is required for this asset recheck");
        return itemRepository.findById(itemId)
                .filter(item -> taskId.equals(item.getInventoryTask().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Asset inventory item not found"));
    }

    private AssetPath findPath(Long taskId, InventoryRecheckItem recheckItem) {
        Long locationId = recheckItem.getLocationId();
        Long floorId = recheckItem.getFloorId();
        Long placeId = recheckItem.getPlaceId();
        if (locationId == null || floorId == null || placeId == null) {
            badRequest("locationId, floorId and placeId are required for a found asset");
        }

        AssetInventoryLocation location = locationRepository.findById(locationId)
                .filter(value -> taskId.equals(value.getInventoryTask().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Asset location not found"));
        AssetInventoryFloor floor = floorRepository.findById(floorId)
                .filter(value -> taskId.equals(value.getInventoryTask().getId())
                        && locationId.equals(value.getLocation().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "Asset floor does not belong to the selected location"));
        AssetInventoryPlace place = placeRepository.findById(placeId)
                .filter(value -> taskId.equals(value.getInventoryTask().getId())
                        && locationId.equals(value.getLocation().getId())
                        && floorId.equals(value.getFloor().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "Asset place does not belong to the selected floor"));
        return new AssetPath(location, floor, place);
    }

    private List<String> mismatchFields(AssetInventoryItem item, AssetPath actual) {
        List<String> fields = new ArrayList<>();
        if (!item.getPlannedLocation().getId().equals(actual.location().getId())) fields.add("LOCATION");
        if (!item.getPlannedFloor().getId().equals(actual.floor().getId())) fields.add("FLOOR");
        if (!item.getPlannedPlace().getId().equals(actual.place().getId())) fields.add("PLACE");
        return fields;
    }

    private Long targetItemId(InventoryRecheckItem item) {
        return item.getResolvedItemId() != null
                ? item.getResolvedItemId()
                : item.getReferenceItemId();
    }

    private void requireMatchingCode(String scannedCode, String expectedCode) {
        if (scannedCode == null || !scannedCode.trim().equalsIgnoreCase(expectedCode)) {
            badRequest("Scanned barcode does not match the recheck item");
        }
    }

    private void badRequest(String message) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    private record AssetPath(
            AssetInventoryLocation location,
            AssetInventoryFloor floor,
            AssetInventoryPlace place
    ) {}
}
