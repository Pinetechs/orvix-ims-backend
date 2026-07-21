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
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskActivityService;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocation;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryScan;
import com.pinetechs.orvix.ims.inventory.vehicle.enums.VehicleInventoryItemStatus;
import com.pinetechs.orvix.ims.inventory.vehicle.enums.VehicleInventoryScanResult;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryScanRepository;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AppVehicleScanService {

    private final AppScanSupport support;
    private final VehicleInventoryItemRepository itemRepository;
    private final VehicleInventoryLocationRepository locationRepository;
    private final VehicleInventoryLocationAssignmentRepository assignmentRepository;
    private final VehicleInventoryScanRepository scanRepository;
    private final InventoryTaskRepository taskRepository;
    private final InventoryTaskActivityService taskActivityService;
    private final UploadedFileService uploadedFileService;

    public AppVehicleScanService(
            AppScanSupport support,
            VehicleInventoryItemRepository itemRepository,
            VehicleInventoryLocationRepository locationRepository,
            VehicleInventoryLocationAssignmentRepository assignmentRepository,
            VehicleInventoryScanRepository scanRepository,
            InventoryTaskRepository taskRepository,
            InventoryTaskActivityService taskActivityService,
            UploadedFileService uploadedFileService
    ) {
        this.support = support;
        this.itemRepository = itemRepository;
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
    public AppScanResponse scan(
            Long taskId,
            AppScanRequest request,
            User user,
            UploadedFile image,
            String payloadHash
    ) {
        InventoryTask task = support.requireAssignedScannableTask(taskId, user, InventoryDomain.VEHICLE);
        String vin = support.requireCode(request.getCode(), 100);
        VehicleInventoryLocation actualLocation = requireAssignedLocation(taskId, request.getLocationId(), user);
        UploadedFile attachedImage = attach(image);

        VehicleInventoryItem item = itemRepository.findForUpdateByTaskIdAndVinNo(taskId, vin).orElse(null);
        VehicleInventoryScan scan = baseScan(task, item, user, vin, request, attachedImage, payloadHash);

        if (item == null) {
            scan.setEventType(InventoryScanEventType.EXTRA);
            scan.setScanResult(VehicleInventoryScanResult.NOT_IN_TASK);
            scan.setActualLocationEntity(actualLocation);
            scan.setActualStoreNo(actualLocation.getStoreNo());
            scan.setActualLocation(actualLocation.getLocationName());
            scan = scanRepository.saveAndFlush(scan);
            startTask(task, user);
            return response(scan, null, "NOT_IN_TASK", "scan.recorded_for_review", true, false);
        }

        if (item.getCurrentScan() != null || item.getStatus() != VehicleInventoryItemStatus.PENDING) {
            boolean sameLocation = equalsIgnoreCase(item.getActualStoreNo(), actualLocation.getStoreNo());
            scan.setEventType(sameLocation ? InventoryScanEventType.DUPLICATE : InventoryScanEventType.CONFLICT);
            scan.setScanResult(sameLocation
                    ? VehicleInventoryScanResult.DUPLICATE_SCAN
                    : VehicleInventoryScanResult.LOCATION_CONFLICT);
            fillExpectedAndActual(scan, item, actualLocation);
            scan = scanRepository.saveAndFlush(scan);
            return response(scan, item,
                    sameLocation ? "DUPLICATE" : "LOCATION_CONFLICT",
                    sameLocation ? "scan.duplicate" : "scan.location_conflict",
                    true, !sameLocation);
        }

        boolean matched = equalsIgnoreCase(item.getStoreNo(), actualLocation.getStoreNo());
        scan.setEventType(InventoryScanEventType.FIRST_SCAN);
        scan.setScanResult(matched ? VehicleInventoryScanResult.FOUND : VehicleInventoryScanResult.WRONG_LOCATION);
        fillExpectedAndActual(scan, item, actualLocation);
        scan = scanRepository.saveAndFlush(scan);

        item.setActualStoreNo(actualLocation.getStoreNo());
        item.setActualLocation(actualLocation.getLocationName());
        item.setStatus(matched ? VehicleInventoryItemStatus.FOUND : VehicleInventoryItemStatus.MISMATCHED);
        item.setCheckedBy(user);
        item.setCheckedAt(LocalDateTime.now());
        item.setCurrentScan(scan);
        itemRepository.save(item);

        adjustFirstScanCounters(taskId, item, matched);
        startTask(task, user);
        return response(scan, item, matched ? "MATCHED" : "LOCATION_MISMATCH",
                matched ? "scan.matched" : "scan.location_mismatch", true, !matched);
    }

    @Transactional
    public AppScanResponse correct(
            Long taskId,
            Long currentScanId,
            AppScanCorrectionRequest request,
            User user,
            UploadedFile image,
            String payloadHash
    ) {
        InventoryTask task = support.requireAssignedScannableTask(taskId, user, InventoryDomain.VEHICLE);
        support.requireCorrectionPermission(user);
        String reason = support.trim(request.getReason(), 1000);
        if (reason == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "Correction reason is required");

        VehicleInventoryScan currentScan = scanRepository.findByIdAndInventoryTaskId(currentScanId, taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Vehicle scan not found"));
        if (currentScan.getItem() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "An extra vehicle scan cannot become an item correction");
        }
        VehicleInventoryItem item = itemRepository
                .findForUpdateByTaskIdAndId(taskId, currentScan.getItem().getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Vehicle inventory item not found"));
        if (item.getCurrentScan() == null || !currentScanId.equals(item.getCurrentScan().getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "The scan is no longer the current accepted scan");
        }
        if (currentScan.getScannedBy() == null || !user.getId().equals(currentScan.getScannedBy().getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Inventory staff can correct only their own scan");
        }

        VehicleInventoryLocation actualLocation = requireAssignedLocation(taskId, request.getLocationId(), user);
        boolean wasMatched = item.getStatus() == VehicleInventoryItemStatus.FOUND;
        boolean nowMatched = equalsIgnoreCase(item.getStoreNo(), actualLocation.getStoreNo());

        AppScanRequest scanRequest = correctionAsScanRequest(request, item.getVinNo());
        VehicleInventoryScan correction = baseScan(task, item, user, item.getVinNo(), scanRequest, attach(image), payloadHash);
        correction.setEventType(InventoryScanEventType.CORRECTION);
        correction.setScanResult(VehicleInventoryScanResult.CORRECTED);
        correction.setCorrectsScan(currentScan);
        correction.setNotes(reason);
        fillExpectedAndActual(correction, item, actualLocation);
        correction = scanRepository.saveAndFlush(correction);

        item.setActualStoreNo(actualLocation.getStoreNo());
        item.setActualLocation(actualLocation.getLocationName());
        item.setStatus(nowMatched ? VehicleInventoryItemStatus.FOUND : VehicleInventoryItemStatus.MISMATCHED);
        item.setCheckedBy(user);
        item.setCheckedAt(LocalDateTime.now());
        item.setCurrentScan(correction);
        itemRepository.save(item);

        int matchedDelta = (nowMatched ? 1 : 0) - (wasMatched ? 1 : 0);
        if (matchedDelta != 0) {
            taskRepository.adjustScanCounters(taskId, 0, matchedDelta);
            plannedLocation(item).ifPresent(location -> locationRepository.adjustScanCounters(location.getId(), 0, matchedDelta));
        }
        return response(correction, item, "CORRECTED", "scan.correction_recorded", true, false);
    }

    private VehicleInventoryLocation requireAssignedLocation(Long taskId, Long locationId, User user) {
        if (locationId == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "Vehicle locationId is required");
        VehicleInventoryLocation location = locationRepository.findById(locationId)
                .filter(value -> taskId.equals(value.getInventoryTask().getId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Vehicle inventory location not found"));
        if (!assignmentRepository.existsActiveByTaskIdAndUserIdAndLocationId(taskId, user.getId(), locationId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Vehicle location is not assigned to the current user");
        }
        return location;
    }

    private VehicleInventoryScan baseScan(InventoryTask task, VehicleInventoryItem item, User user, String vin,
                                          AppScanRequest request, UploadedFile image, String payloadHash) {
        VehicleInventoryScan scan = new VehicleInventoryScan();
        scan.setInventoryTask(task);
        scan.setItem(item);
        scan.setScannedBy(user);
        scan.setScannedVin(vin);
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

    private void fillExpectedAndActual(VehicleInventoryScan scan, VehicleInventoryItem item,
                                       VehicleInventoryLocation actualLocation) {
        scan.setExpectedStoreNo(item.getStoreNo());
        scan.setExpectedLocation(item.getLocation());
        scan.setActualLocationEntity(actualLocation);
        scan.setActualStoreNo(actualLocation.getStoreNo());
        scan.setActualLocation(actualLocation.getLocationName());
    }

    private void adjustFirstScanCounters(Long taskId, VehicleInventoryItem item, boolean matched) {
        taskRepository.adjustScanCounters(taskId, 1, matched ? 1 : 0);
        VehicleInventoryLocation planned = plannedLocation(item)
                .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "Planned vehicle location is missing"));
        locationRepository.adjustScanCounters(planned.getId(), 1, matched ? 1 : 0);
    }

    private Optional<VehicleInventoryLocation> plannedLocation(VehicleInventoryItem item) {
        return locationRepository.findByInventoryTaskIdAndStoreNo(item.getInventoryTask().getId(), item.getStoreNo());
    }

    private void startTask(InventoryTask task, User user) {
        taskActivityService.startOnFirstScan(task, user);
    }

    private UploadedFile attach(UploadedFile image) {
        return image == null ? null : uploadedFileService.markAsAttached(image.getId());
    }


    /// Replay the scan if the payload hash matches, otherwise throw a conflict exception
    private AppScanResponse replayResponse(VehicleInventoryScan scan, String payloadHash) {
        if (!scan.getPayloadHash().equals(payloadHash)) {
            throw new BusinessException(HttpStatus.CONFLICT, "clientScanId was already used with different scan data");
        }
        VehicleInventoryItem item = scan.getItem();
        String result = switch (scan.getEventType()) {
            case EXTRA -> "NOT_IN_TASK";
            case DUPLICATE -> "DUPLICATE";
            case CONFLICT -> "LOCATION_CONFLICT";
            case CORRECTION -> "CORRECTED";
            default -> scan.getScanResult() == VehicleInventoryScanResult.FOUND ? "MATCHED" : "LOCATION_MISMATCH";
        };
        String message = switch (scan.getEventType()) {
            case EXTRA -> "scan.recorded_for_review";
            case DUPLICATE -> "scan.duplicate";
            case CONFLICT -> "scan.location_conflict";
            case CORRECTION -> "scan.correction_recorded";
            default -> scan.getScanResult() == VehicleInventoryScanResult.FOUND ? "scan.matched" : "scan.location_mismatch";
        };
        boolean correctionAllowed = scan.getEventType() == InventoryScanEventType.CONFLICT
                || (scan.getEventType() == InventoryScanEventType.FIRST_SCAN
                    && scan.getScanResult() == VehicleInventoryScanResult.WRONG_LOCATION);
        AppScanResponse response = response(scan, item, result, message, true, correctionAllowed);
        response.setIdempotentReplay(true);
        return response;
    }

    private AppScanResponse response(VehicleInventoryScan scan, VehicleInventoryItem item, String result,
                                     String messageKey, boolean accepted, boolean correctionAllowed) {
        AppScanResponse response = new AppScanResponse();
        response.setScanId(scan.getId());
        response.setItemId(item == null ? null : item.getId());
        response.setCurrentAcceptedScanId(item == null || item.getCurrentScan() == null
                ? null : item.getCurrentScan().getId());
        response.setImageFileId(scan.getScanImage() == null ? null : scan.getScanImage().getId());
        response.setInventoryDomain(InventoryDomain.VEHICLE);
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
            VehicleInventoryScan scan,
            VehicleInventoryItem item
    ) {
        AppScanItemSummaryResponse summary = new AppScanItemSummaryResponse();
        String vin = item == null ? scan.getScannedVin() : item.getVinNo();
        summary.setCode(item == null || item.getPartNo() == null ? vin : item.getPartNo());
        summary.setBarcode(vin);
        if (item == null) {
            summary.setDisplayName(vin);
            return summary;
        }
        String vehicleName = joinNotBlank(item.getMake(), item.getModelName());
        summary.setDisplayName(vehicleName == null ? vin : vehicleName);
        summary.setType("VEHICLE");
        summary.setMake(item.getMake());
        summary.setModel(item.getModelName());
        summary.setModelYear(item.getModelYear());
        summary.setColor(item.getColorNo());
        return summary;
    }

    private AppScanLocationSummaryResponse actualLocationSummary(VehicleInventoryScan scan) {
        AppScanLocationSummaryResponse summary = new AppScanLocationSummaryResponse();
        if (scan.getActualLocationEntity() != null) {
            summary.setLocationId(scan.getActualLocationEntity().getId());
        }
        summary.setLocationCode(scan.getActualStoreNo());
        summary.setLocationName(scan.getActualLocation());
        return summary;
    }

    private String joinNotBlank(String first, String second) {
        String left = first == null ? "" : first.trim();
        String right = second == null ? "" : second.trim();
        String joined = (left + " " + right).trim();
        return joined.isEmpty() ? null : joined;
    }

    private AppScanRequest correctionAsScanRequest(AppScanCorrectionRequest correction, String code) {
        AppScanRequest request = new AppScanRequest();
        request.setClientScanId(correction.getClientScanId());
        request.setCode(code);
        request.setLocationId(correction.getLocationId());
        request.setDeviceScannedAt(correction.getDeviceScannedAt());
        request.setDeviceId(correction.getDeviceId());
        request.setSymbology(correction.getSymbology());
        request.setImageSource(correction.getImageSource());
        return request;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }
}
