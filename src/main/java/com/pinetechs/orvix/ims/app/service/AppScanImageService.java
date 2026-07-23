package com.pinetechs.orvix.ims.app.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryScanRepository;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryScanRepository;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.review.repository.InventoryRecheckItemRepository;
import com.pinetechs.orvix.ims.inventory.review.service.ReviewCenterService;
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
    private final InventoryTaskRepository taskRepository;
    private final InventoryRecheckItemRepository recheckItemRepository;

    public AppScanImageService(
            InventoryTaskAssignmentRepository assignmentRepository,
            AccessPolicyService accessPolicyService,
            VehicleInventoryScanRepository vehicleScanRepository,
            AssetInventoryScanRepository assetScanRepository,
            SparePartInventoryScanRepository sparePartScanRepository,
            InventoryTaskRepository taskRepository,
            InventoryRecheckItemRepository recheckItemRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.accessPolicyService = accessPolicyService;
        this.vehicleScanRepository = vehicleScanRepository;
        this.assetScanRepository = assetScanRepository;
        this.sparePartScanRepository = sparePartScanRepository;
        this.taskRepository = taskRepository;
        this.recheckItemRepository = recheckItemRepository;
    }

    @Transactional(readOnly = true)
    public UploadedFile getAuthorizedImage(Long taskId, Long scanId, User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        accessPolicyService.assertCanUseApp(user);
        InventoryTask task = assignmentRepository
                .findActiveByTaskIdAndUserIdWithTaskAndCompany(taskId, user.getId())
                .map(InventoryTaskAssignment::getInventoryTask)
                .orElseGet(() -> requireRecheckTask(taskId, scanId, user));

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

    private InventoryTask requireRecheckTask(Long taskId, Long scanId, User user) {
        boolean assignedRecheck = recheckItemRepository.existsAssignedActiveRecheckForScan(
                taskId,
                scanId,
                user.getId(),
                ReviewCenterService.ACTIVE_RECHECK_STATUSES
        );
        if (!assignedRecheck) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "Assigned inventory task or recheck request not found");
        }
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Inventory task not found"));
    }
}
