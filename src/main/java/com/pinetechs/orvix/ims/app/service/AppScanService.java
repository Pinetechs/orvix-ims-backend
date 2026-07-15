package com.pinetechs.orvix.ims.app.service;

import com.pinetechs.orvix.ims.app.dto.AppScanCorrectionRequest;
import com.pinetechs.orvix.ims.app.dto.AppScanRequest;
import com.pinetechs.orvix.ims.app.dto.AppScanResponse;
import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.file.service.UploadedFileService;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

@Service
public class AppScanService {

    private final AppScanSupport support;
    private final AppVehicleScanService vehicleScanService;
    private final AppAssetScanService assetScanService;
    private final AppSparePartScanService sparePartScanService;
    private final UploadedFileService uploadedFileService;

    public AppScanService(
            AppScanSupport support,
            AppVehicleScanService vehicleScanService,
            AppAssetScanService assetScanService,
            AppSparePartScanService sparePartScanService,
            UploadedFileService uploadedFileService
    ) {
        this.support = support;
        this.vehicleScanService = vehicleScanService;
        this.assetScanService = assetScanService;
        this.sparePartScanService = sparePartScanService;
        this.uploadedFileService = uploadedFileService;
    }

    public AppScanResponse scan(Long taskId, AppScanRequest request, MultipartFile image, User user) throws IOException {
        if (request == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "Scan request is required");

        support.validateClientScanId(request.getClientScanId());
        request.setClientScanId(request.getClientScanId().trim().toLowerCase(Locale.ROOT));

        InventoryTask task = support.requireAssignedScannableTask(taskId, user);


        validateDomainScanRequest(task.getInventoryDomain(), request);
        support.requireImageWhenConfigured(task, image);

        String payloadHash = support.scanFingerprint(request, image);

        Optional<AppScanResponse> replay = replay(task.getInventoryDomain(), taskId, request.getClientScanId(), payloadHash);
        if (replay.isPresent()) return replay.get();

        UploadedFile uploadedImage = uploadImage(task, image, user);
        try {
            return switch (task.getInventoryDomain()) {
                case VEHICLE -> vehicleScanService.scan(taskId, request, user, uploadedImage, payloadHash);
                case ASSET -> assetScanService.scan(taskId, request, user, uploadedImage, payloadHash);
                case SPARE_PART -> sparePartScanService.scan(taskId, request, user, uploadedImage, payloadHash);
            };
        } catch (DataIntegrityViolationException ex) {
            return replay(task.getInventoryDomain(), taskId, request.getClientScanId(), payloadHash)
                    .orElseThrow(() -> ex);
        }
    }

    public AppScanResponse correct(Long taskId, Long currentScanId, AppScanCorrectionRequest request,
                                   MultipartFile image, User user) throws IOException {
        if (request == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "Correction request is required");
        if (currentScanId == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "currentScanId is required");
        support.validateClientScanId(request.getClientScanId());
        request.setClientScanId(request.getClientScanId().trim().toLowerCase(Locale.ROOT));
        InventoryTask task = support.requireAssignedScannableTask(taskId, user);
        validateDomainCorrectionRequest(task.getInventoryDomain(), request);
        support.requireCorrectionPermission(user);
        support.requireImageWhenConfigured(task, image);
        String payloadHash = support.correctionFingerprint(currentScanId, request, image);

        Optional<AppScanResponse> replay = replay(task.getInventoryDomain(), taskId, request.getClientScanId(), payloadHash);
        if (replay.isPresent()) return replay.get();

        UploadedFile uploadedImage = uploadImage(task, image, user);
        try {
            return switch (task.getInventoryDomain()) {
                case VEHICLE -> vehicleScanService.correct(taskId, currentScanId, request, user, uploadedImage, payloadHash);
                case ASSET -> assetScanService.correct(taskId, currentScanId, request, user, uploadedImage, payloadHash);
                case SPARE_PART -> sparePartScanService.correct(taskId, currentScanId, request, user, uploadedImage, payloadHash);
            };
        } catch (DataIntegrityViolationException ex) {
            return replay(task.getInventoryDomain(), taskId, request.getClientScanId(), payloadHash)
                    .orElseThrow(() -> ex);
        }
    }

    private Optional<AppScanResponse> replay(InventoryDomain domain, Long taskId, String clientScanId, String payloadHash) {
        return switch (domain) {
            case VEHICLE -> vehicleScanService.replay(taskId, clientScanId.trim(), payloadHash);
            case ASSET -> assetScanService.replay(taskId, clientScanId.trim(), payloadHash);
            case SPARE_PART -> sparePartScanService.replay(taskId, clientScanId.trim(), payloadHash);
        };
    }

    private UploadedFile uploadImage(InventoryTask task, MultipartFile image, User user) throws IOException {
        if (image == null || image.isEmpty()) return null;
        String domainFolder = task.getInventoryDomain().name().toLowerCase(Locale.ROOT).replace('_', '-');
        String folder = "inventory-tasks/" + task.getId() + "/scans/" + domainFolder;
        return uploadedFileService.uploadPrivateScanImage(image, folder, user);
    }

    private void validateDomainScanRequest(InventoryDomain domain, AppScanRequest request) {
        if (domain == InventoryDomain.VEHICLE) {
            support.requireCode(request.getCode(), 100);
            if (request.getLocationId() == null) badRequest("locationId is required for vehicle scan");
            if (request.getBranchId() != null || request.getFloorId() != null
                    || request.getPlaceId() != null || request.getCountedQty() != null) {
                badRequest("Vehicle scan contains fields that do not belong to the vehicle domain");
            }
            return;
        }
        if (domain == InventoryDomain.ASSET) {
            support.requireCode(request.getCode(), 100);
            if (request.getLocationId() == null || request.getFloorId() == null || request.getPlaceId() == null) {
                badRequest("locationId, floorId and placeId are required for asset scan");
            }
            if (request.getBranchId() != null || request.getCountedQty() != null) {
                badRequest("Asset scan contains fields that do not belong to the asset domain");
            }
            return;
        }
        support.requireCode(request.getCode(), 150);
        if (request.getBranchId() == null || request.getLocationId() == null || request.getCountedQty() == null) {
            badRequest("branchId, locationId and countedQty are required for spare part scan");
        }
        if (request.getFloorId() != null || request.getPlaceId() != null) {
            badRequest("Spare part scan contains fields that do not belong to the spare part domain");
        }
    }

    private void validateDomainCorrectionRequest(InventoryDomain domain, AppScanCorrectionRequest request) {
        if (request.getReason() == null || request.getReason().isBlank()) {
            badRequest("Correction reason is required");
        }
        if (domain == InventoryDomain.VEHICLE) {
            if (request.getLocationId() == null) badRequest("locationId is required for vehicle correction");
            if (request.getBranchId() != null || request.getFloorId() != null
                    || request.getPlaceId() != null || request.getCountedQty() != null) {
                badRequest("Vehicle correction contains fields that do not belong to the vehicle domain");
            }
            return;
        }
        if (domain == InventoryDomain.ASSET) {
            if (request.getLocationId() == null || request.getFloorId() == null || request.getPlaceId() == null) {
                badRequest("locationId, floorId and placeId are required for asset correction");
            }
            if (request.getBranchId() != null || request.getCountedQty() != null) {
                badRequest("Asset correction contains fields that do not belong to the asset domain");
            }
            return;
        }
        if (request.getBranchId() == null || request.getLocationId() == null || request.getCountedQty() == null) {
            badRequest("branchId, locationId and countedQty are required for spare part correction");
        }
        if (request.getFloorId() != null || request.getPlaceId() != null) {
            badRequest("Spare part correction contains fields that do not belong to the spare part domain");
        }
    }

    private void badRequest(String message) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, message);
    }
}
