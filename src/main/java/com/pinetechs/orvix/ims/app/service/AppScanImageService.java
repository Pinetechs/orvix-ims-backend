package com.pinetechs.orvix.ims.app.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryScanRepository;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryScanRepository;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryScanRepository;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppScanImageService {

    private final InventoryTaskAssignmentRepository assignmentRepository;
    private final AccessPolicyService accessPolicyService;
    private final VehicleInventoryScanRepository vehicleScanRepository;
    private final AssetInventoryScanRepository assetScanRepository;
    private final SparePartInventoryScanRepository sparePartScanRepository;

    public AppScanImageService(
            InventoryTaskAssignmentRepository assignmentRepository,
            AccessPolicyService accessPolicyService,
            VehicleInventoryScanRepository vehicleScanRepository,
            AssetInventoryScanRepository assetScanRepository,
            SparePartInventoryScanRepository sparePartScanRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.accessPolicyService = accessPolicyService;
        this.vehicleScanRepository = vehicleScanRepository;
        this.assetScanRepository = assetScanRepository;
        this.sparePartScanRepository = sparePartScanRepository;
    }

    @Transactional(readOnly = true)
    public UploadedFile getAuthorizedImage(Long taskId, Long scanId, User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        accessPolicyService.assertCanUseApp(user);
        InventoryTaskAssignment assignment = assignmentRepository
                .findActiveByTaskIdAndUserIdWithTaskAndCompany(taskId, user.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Assigned inventory task not found"));
        InventoryTask task = assignment.getInventoryTask();

        UploadedFile file = switch (task.getInventoryDomain()) {
            case VEHICLE -> vehicleScanRepository.findByIdAndInventoryTaskId(scanId, taskId)
                    .map(scan -> scan.getScanImage()).orElse(null);
            case ASSET -> assetScanRepository.findByIdAndInventoryTaskId(scanId, taskId)
                    .map(scan -> scan.getScanImage()).orElse(null);
            case SPARE_PART -> sparePartScanRepository.findByIdAndInventoryTaskId(scanId, taskId)
                    .map(scan -> scan.getScanImage()).orElse(null);
        };
        if (file == null || Boolean.TRUE.equals(file.getDeleted()) || Boolean.TRUE.equals(file.getTemp())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Scan image not found");
        }
        return file;
    }
}
