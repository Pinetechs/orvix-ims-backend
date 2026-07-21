package com.pinetechs.orvix.ims.app.service;

import com.pinetechs.orvix.ims.app.dto.AppScanCorrectionRequest;
import com.pinetechs.orvix.ims.app.dto.AppScanItemSummaryResponse;
import com.pinetechs.orvix.ims.app.dto.AppScanLocationSummaryResponse;
import com.pinetechs.orvix.ims.app.dto.AppScanRequest;
import com.pinetechs.orvix.ims.app.dto.AppScanResponse;
import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.file.service.UploadedFileService;
import com.pinetechs.orvix.ims.inventory.asset.entity.*;
import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryItemStatus;
import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryScanResult;
import com.pinetechs.orvix.ims.inventory.asset.repository.*;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskActivityService;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class AppAssetScanService {

    private final AppScanSupport support;
    private final AssetInventoryItemRepository itemRepository;
    private final AssetInventoryLocationRepository locationRepository;
    private final AssetInventoryFloorRepository floorRepository;
    private final AssetInventoryPlaceRepository placeRepository;
    private final AssetInventoryLocationAssignmentRepository assignmentRepository;
    private final AssetInventoryScanRepository scanRepository;
    private final InventoryTaskRepository taskRepository;
    private final InventoryTaskActivityService taskActivityService;
    private final UploadedFileService uploadedFileService;

    public AppAssetScanService(
            AppScanSupport support,
            AssetInventoryItemRepository itemRepository,
            AssetInventoryLocationRepository locationRepository,
            AssetInventoryFloorRepository floorRepository,
            AssetInventoryPlaceRepository placeRepository,
            AssetInventoryLocationAssignmentRepository assignmentRepository,
            AssetInventoryScanRepository scanRepository,
            InventoryTaskRepository taskRepository,
            InventoryTaskActivityService taskActivityService,
            UploadedFileService uploadedFileService
    ) {
        this.support = support;
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.floorRepository = floorRepository;
        this.placeRepository = placeRepository;
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
    public AppScanResponse scan(Long taskId, AppScanRequest request, User user,
                                UploadedFile image, String payloadHash) {
        InventoryTask task = support.requireAssignedScannableTask(taskId, user, InventoryDomain.ASSET);
        String barcode = support.requireCode(request.getCode(), 100);
        AssetPath actual = requireAssignedPath(taskId, request.getLocationId(), request.getFloorId(), request.getPlaceId(), user);
        AssetInventoryItem item = itemRepository.findForUpdateByTaskIdAndBarcode(taskId, barcode).orElse(null);
        AssetInventoryScan scan = baseScan(task, item, user, barcode, request, attach(image), payloadHash);

        if (item == null) {
            scan.setEventType(InventoryScanEventType.EXTRA);
            scan.setScanResult(AssetInventoryScanResult.EXTRA);
            setActual(scan, actual);
            scan = scanRepository.saveAndFlush(scan);
            startTask(task, user);
            return response(scan, null, "EXTRA", "scan.recorded_for_review", List.of(), true, false);
        }

        List<String> mismatches = mismatchFields(item, actual);
        boolean sameCanonicalPath = sameCanonicalPath(item, actual);
        if (item.getCurrentScan() != null || item.getStatus() != AssetInventoryItemStatus.NOT_SCANNED) {
            scan.setEventType(sameCanonicalPath ? InventoryScanEventType.DUPLICATE : InventoryScanEventType.CONFLICT);
            scan.setScanResult(sameCanonicalPath
                    ? AssetInventoryScanResult.DUPLICATE_SAME_LOCATION
                    : AssetInventoryScanResult.DUPLICATE_DIFFERENT_LOCATION);
            setExpectedAndActual(scan, item, actual, mismatches);
            scan = scanRepository.saveAndFlush(scan);
            return response(scan, item, sameCanonicalPath ? "DUPLICATE" : "LOCATION_CONFLICT",
                    sameCanonicalPath ? "scan.duplicate" : "scan.location_conflict",
                    mismatches, true, !sameCanonicalPath);
        }

        boolean matched = mismatches.isEmpty();
        scan.setEventType(InventoryScanEventType.FIRST_SCAN);
        scan.setScanResult(matched ? AssetInventoryScanResult.MATCHED : AssetInventoryScanResult.LOCATION_MISMATCH);
        setExpectedAndActual(scan, item, actual, mismatches);
        scan = scanRepository.saveAndFlush(scan);

        applyCanonicalItem(item, actual, user, scan, matched);
        itemRepository.save(item);
        taskRepository.adjustScanCounters(taskId, 1, matched ? 1 : 0);
        locationRepository.adjustScanCounters(item.getPlannedLocation().getId(), 1, matched ? 1 : 0);
        startTask(task, user);
        return response(scan, item, matched ? "MATCHED" : "LOCATION_MISMATCH",
                matched ? "scan.matched" : "scan.location_mismatch", mismatches, true, !matched);
    }

    @Transactional
    public AppScanResponse correct(Long taskId, Long currentScanId, AppScanCorrectionRequest request,
                                   User user, UploadedFile image, String payloadHash) {
        InventoryTask task = support.requireAssignedScannableTask(taskId, user, InventoryDomain.ASSET);
        support.requireCorrectionPermission(user);
        String reason = support.trim(request.getReason(), 1000);
        if (reason == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "Correction reason is required");

        AssetInventoryScan currentScan = scanRepository.findByIdAndInventoryTaskId(currentScanId, taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Asset scan not found"));
        if (currentScan.getItem() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "An extra asset scan cannot become an item correction");
        }
        AssetInventoryItem item = itemRepository
                .findForUpdateByTaskIdAndId(taskId, currentScan.getItem().getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Asset inventory item not found"));
        requireCurrentOwnedScan(item, currentScan, currentScanId, user);

        AssetPath actual = requireAssignedPath(taskId, request.getLocationId(), request.getFloorId(), request.getPlaceId(), user);
        List<String> mismatches = mismatchFields(item, actual);
        boolean wasMatched = item.getStatus() == AssetInventoryItemStatus.MATCHED;
        boolean nowMatched = mismatches.isEmpty();

        AppScanRequest scanRequest = correctionAsScanRequest(request, item.getBarcode());
        AssetInventoryScan correction = baseScan(task, item, user, item.getBarcode(), scanRequest, attach(image), payloadHash);
        correction.setEventType(InventoryScanEventType.CORRECTION);
        correction.setScanResult(nowMatched ? AssetInventoryScanResult.MATCHED : AssetInventoryScanResult.LOCATION_MISMATCH);
        correction.setCorrectsScan(currentScan);
        correction.setNotes(reason);
        setExpectedAndActual(correction, item, actual, mismatches);
        correction = scanRepository.saveAndFlush(correction);

        applyCanonicalItem(item, actual, user, correction, nowMatched);
        itemRepository.save(item);
        int matchedDelta = (nowMatched ? 1 : 0) - (wasMatched ? 1 : 0);
        if (matchedDelta != 0) {
            taskRepository.adjustScanCounters(taskId, 0, matchedDelta);
            locationRepository.adjustScanCounters(item.getPlannedLocation().getId(), 0, matchedDelta);
        }
        return response(correction, item, "CORRECTED", "scan.correction_recorded",
                mismatches, true, false);
    }

    private AssetPath requireAssignedPath(Long taskId, Long locationId, Long floorId, Long placeId, User user) {
        if (locationId == null || floorId == null || placeId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "locationId, floorId and placeId are required for asset scan");
        }
        AssetInventoryLocation location = locationRepository.findById(locationId)
                .filter(value -> taskId.equals(value.getInventoryTask().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Asset location not found"));
        if (!assignmentRepository.existsActiveByTaskIdAndUserIdAndLocationId(taskId, user.getId(), locationId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Asset location is not assigned to the current user");
        }
        AssetInventoryFloor floor = floorRepository.findById(floorId)
                .filter(value -> taskId.equals(value.getInventoryTask().getId())
                        && locationId.equals(value.getLocation().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Asset floor does not belong to the selected location"));
        AssetInventoryPlace place = placeRepository.findById(placeId)
                .filter(value -> taskId.equals(value.getInventoryTask().getId())
                        && locationId.equals(value.getLocation().getId())
                        && floorId.equals(value.getFloor().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Asset place does not belong to the selected floor"));
        return new AssetPath(location, floor, place);
    }

    private AssetInventoryScan baseScan(InventoryTask task, AssetInventoryItem item, User user, String barcode,
                                        AppScanRequest request, UploadedFile image, String payloadHash) {
        AssetInventoryScan scan = new AssetInventoryScan();
        scan.setInventoryTask(task);
        scan.setItem(item);
        scan.setScannedBy(user);
        scan.setScannedBarcode(barcode);
        scan.setClientScanId(request.getClientScanId().trim());
        scan.setPayloadHash(payloadHash);
        scan.setScanImage(image);
        scan.setDeviceScannedAt(request.getDeviceScannedAt());
        scan.setDeviceId(support.trim(request.getDeviceId(), 150));
        scan.setSymbology(support.trim(request.getSymbology(), 80));
        scan.setImageSource(support.imageSource(request.getImageSource()));
        scan.setNotes(support.trim(request.getNotes(), 1000));
        return scan;
    }

    private void setExpectedAndActual(AssetInventoryScan scan, AssetInventoryItem item,
                                      AssetPath actual, List<String> mismatches) {
        scan.setExpectedLocation(item.getPlannedLocation());
        scan.setExpectedFloor(item.getPlannedFloor());
        scan.setExpectedPlace(item.getPlannedPlace());
        setActual(scan, actual);
        scan.setMismatchFields(String.join(",", mismatches));
    }

    private void setActual(AssetInventoryScan scan, AssetPath actual) {
        scan.setActualLocation(actual.location());
        scan.setActualFloor(actual.floor());
        scan.setActualPlace(actual.place());
    }

    private List<String> mismatchFields(AssetInventoryItem item, AssetPath actual) {
        List<String> fields = new ArrayList<>();
        if (!item.getPlannedLocation().getId().equals(actual.location().getId())) fields.add("LOCATION");
        if (!item.getPlannedFloor().getId().equals(actual.floor().getId())) fields.add("FLOOR");
        if (!item.getPlannedPlace().getId().equals(actual.place().getId())) fields.add("PLACE");
        return fields;
    }

    private boolean sameCanonicalPath(AssetInventoryItem item, AssetPath actual) {
        return item.getActualLocation() != null && item.getActualFloor() != null && item.getActualPlace() != null
                && item.getActualLocation().getId().equals(actual.location().getId())
                && item.getActualFloor().getId().equals(actual.floor().getId())
                && item.getActualPlace().getId().equals(actual.place().getId());
    }

    private void applyCanonicalItem(AssetInventoryItem item, AssetPath actual, User user,
                                    AssetInventoryScan scan, boolean matched) {
        item.setActualLocation(actual.location());
        item.setActualFloor(actual.floor());
        item.setActualPlace(actual.place());
        item.setStatus(matched ? AssetInventoryItemStatus.MATCHED : AssetInventoryItemStatus.LOCATION_MISMATCH);
        item.setCheckedBy(user);
        item.setCheckedAt(LocalDateTime.now());
        item.setCurrentScan(scan);
    }

    private void requireCurrentOwnedScan(AssetInventoryItem item, AssetInventoryScan scan, Long scanId, User user) {
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

    private AppScanResponse replayResponse(AssetInventoryScan scan, String payloadHash) {
        if (!scan.getPayloadHash().equals(payloadHash)) {
            throw new BusinessException(HttpStatus.CONFLICT, "clientScanId was already used with different scan data");
        }
        List<String> fields = scan.getMismatchFields() == null || scan.getMismatchFields().isBlank()
                ? List.of() : Arrays.asList(scan.getMismatchFields().split(","));
        String result = switch (scan.getEventType()) {
            case EXTRA -> "EXTRA";
            case DUPLICATE -> "DUPLICATE";
            case CONFLICT -> "LOCATION_CONFLICT";
            case CORRECTION -> "CORRECTED";
            default -> scan.getScanResult() == AssetInventoryScanResult.MATCHED ? "MATCHED" : "LOCATION_MISMATCH";
        };
        String message = switch (scan.getEventType()) {
            case EXTRA -> "scan.recorded_for_review";
            case DUPLICATE -> "scan.duplicate";
            case CONFLICT -> "scan.location_conflict";
            case CORRECTION -> "scan.correction_recorded";
            default -> scan.getScanResult() == AssetInventoryScanResult.MATCHED ? "scan.matched" : "scan.location_mismatch";
        };
        boolean correctionAllowed = scan.getEventType() == InventoryScanEventType.CONFLICT
                || (scan.getEventType() == InventoryScanEventType.FIRST_SCAN
                    && scan.getScanResult() == AssetInventoryScanResult.LOCATION_MISMATCH);
        AppScanResponse response = response(scan, scan.getItem(), result, message, fields, true, correctionAllowed);
        response.setIdempotentReplay(true);
        return response;
    }

    private AppScanResponse response(AssetInventoryScan scan, AssetInventoryItem item, String result,
                                     String messageKey, List<String> fields, boolean accepted, boolean correctionAllowed) {
        AppScanResponse response = new AppScanResponse();
        response.setScanId(scan.getId());
        response.setItemId(item == null ? null : item.getId());
        response.setCurrentAcceptedScanId(item == null || item.getCurrentScan() == null
                ? null : item.getCurrentScan().getId());
        response.setImageFileId(scan.getScanImage() == null ? null : scan.getScanImage().getId());
        response.setInventoryDomain(InventoryDomain.ASSET);
        response.setClientScanId(scan.getClientScanId());
        response.setEventType(scan.getEventType().name());
        response.setResultCode(result);
        response.setMessageKey(messageKey);
        response.setMismatchFields(fields);
        response.setAccepted(accepted);
        response.setCorrectionAllowed(correctionAllowed);
        response.setServerScannedAt(scan.getScannedAt());
        response.setItem(itemSummary(scan, item));
        response.setActualLocation(actualLocationSummary(scan));
        return response;
    }

    private AppScanItemSummaryResponse itemSummary(
            AssetInventoryScan scan,
            AssetInventoryItem item
    ) {
        AppScanItemSummaryResponse summary = new AppScanItemSummaryResponse();
        String barcode = item == null ? scan.getScannedBarcode() : item.getBarcode();
        summary.setCode(barcode);
        summary.setBarcode(barcode);
        if (item == null) {
            summary.setDisplayName(barcode);
            return summary;
        }
        summary.setDisplayName(firstNotBlank(
                item.getDescription(), item.getAssetType(), item.getAssetCategory(), barcode));
        summary.setCategory(item.getAssetCategory());
        summary.setType(item.getAssetType());
        summary.setCondition(item.getAssetCondition());
        return summary;
    }

    private AppScanLocationSummaryResponse actualLocationSummary(AssetInventoryScan scan) {
        AppScanLocationSummaryResponse summary = new AppScanLocationSummaryResponse();
        if (scan.getActualLocation() != null) {
            summary.setLocationId(scan.getActualLocation().getId());
            summary.setLocationName(scan.getActualLocation().getLocationName());
        }
        if (scan.getActualFloor() != null) {
            summary.setFloorId(scan.getActualFloor().getId());
            summary.setFloorName(scan.getActualFloor().getFloorName());
        }
        if (scan.getActualPlace() != null) {
            summary.setPlaceId(scan.getActualPlace().getId());
            summary.setPlaceName(scan.getActualPlace().getPlaceName());
        }
        return summary;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private AppScanRequest correctionAsScanRequest(AppScanCorrectionRequest correction, String code) {
        AppScanRequest request = new AppScanRequest();
        request.setClientScanId(correction.getClientScanId());
        request.setCode(code);
        request.setLocationId(correction.getLocationId());
        request.setFloorId(correction.getFloorId());
        request.setPlaceId(correction.getPlaceId());
        request.setDeviceScannedAt(correction.getDeviceScannedAt());
        request.setDeviceId(correction.getDeviceId());
        request.setSymbology(correction.getSymbology());
        request.setImageSource(correction.getImageSource());
        return request;
    }

    private record AssetPath(AssetInventoryLocation location, AssetInventoryFloor floor, AssetInventoryPlace place) {}
}
