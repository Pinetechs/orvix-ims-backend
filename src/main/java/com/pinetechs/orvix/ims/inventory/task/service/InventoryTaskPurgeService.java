package com.pinetechs.orvix.ims.inventory.task.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.file.service.UploadedFileService;
import com.pinetechs.orvix.ims.inventory.asset.repository.*;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.*;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.*;
import com.pinetechs.orvix.ims.jobs.entity.BackgroundJob;
import com.pinetechs.orvix.ims.jobs.enums.JobStatus;
import com.pinetechs.orvix.ims.jobs.repository.BackgroundJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class InventoryTaskPurgeService {

    public static final long HARD_DELETE_SCAN_LIMIT = 10L;

    private final InventoryTaskRepository taskRepository;
    private final InventoryTaskAssignmentRepository taskAssignmentRepository;
    private final VehicleInventoryScanRepository vehicleScanRepository;
    private final VehicleInventoryLocationAssignmentRepository vehicleAssignmentRepository;
    private final VehicleInventoryItemRepository vehicleItemRepository;
    private final VehicleInventoryLocationRepository vehicleLocationRepository;
    private final AssetInventoryScanRepository assetScanRepository;
    private final AssetInventoryLocationAssignmentRepository assetAssignmentRepository;
    private final AssetInventoryItemRepository assetItemRepository;
    private final AssetInventoryPlaceRepository assetPlaceRepository;
    private final AssetInventoryFloorRepository assetFloorRepository;
    private final AssetInventoryLocationRepository assetLocationRepository;
    private final AssetInventoryCategoryRepository assetCategoryRepository;
    private final SparePartInventoryScanRepository spareScanRepository;
    private final SparePartInventoryBranchAssignmentRepository spareAssignmentRepository;
    private final SparePartInventoryItemRepository spareItemRepository;
    private final SparePartInventoryLocationRepository spareLocationRepository;
    private final SparePartInventoryBranchRepository spareBranchRepository;
    private final SparePartInventoryBrandRepository spareBrandRepository;
    private final BackgroundJobRepository backgroundJobRepository;
    private final UploadedFileService uploadedFileService;

    public InventoryTaskPurgeService(
            InventoryTaskRepository taskRepository,
            InventoryTaskAssignmentRepository taskAssignmentRepository,
            VehicleInventoryScanRepository vehicleScanRepository,
            VehicleInventoryLocationAssignmentRepository vehicleAssignmentRepository,
            VehicleInventoryItemRepository vehicleItemRepository,
            VehicleInventoryLocationRepository vehicleLocationRepository,
            AssetInventoryScanRepository assetScanRepository,
            AssetInventoryLocationAssignmentRepository assetAssignmentRepository,
            AssetInventoryItemRepository assetItemRepository,
            AssetInventoryPlaceRepository assetPlaceRepository,
            AssetInventoryFloorRepository assetFloorRepository,
            AssetInventoryLocationRepository assetLocationRepository,
            AssetInventoryCategoryRepository assetCategoryRepository,
            SparePartInventoryScanRepository spareScanRepository,
            SparePartInventoryBranchAssignmentRepository spareAssignmentRepository,
            SparePartInventoryItemRepository spareItemRepository,
            SparePartInventoryLocationRepository spareLocationRepository,
            SparePartInventoryBranchRepository spareBranchRepository,
            SparePartInventoryBrandRepository spareBrandRepository,
            BackgroundJobRepository backgroundJobRepository,
            UploadedFileService uploadedFileService
    ) {
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.vehicleScanRepository = vehicleScanRepository;
        this.vehicleAssignmentRepository = vehicleAssignmentRepository;
        this.vehicleItemRepository = vehicleItemRepository;
        this.vehicleLocationRepository = vehicleLocationRepository;
        this.assetScanRepository = assetScanRepository;
        this.assetAssignmentRepository = assetAssignmentRepository;
        this.assetItemRepository = assetItemRepository;
        this.assetPlaceRepository = assetPlaceRepository;
        this.assetFloorRepository = assetFloorRepository;
        this.assetLocationRepository = assetLocationRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.spareScanRepository = spareScanRepository;
        this.spareAssignmentRepository = spareAssignmentRepository;
        this.spareItemRepository = spareItemRepository;
        this.spareLocationRepository = spareLocationRepository;
        this.spareBranchRepository = spareBranchRepository;
        this.spareBrandRepository = spareBrandRepository;
        this.backgroundJobRepository = backgroundJobRepository;
        this.uploadedFileService = uploadedFileService;
    }

    @Transactional(readOnly = true)
    public long countScans(InventoryTask task) {
        if (task == null || task.getId() == null || task.getInventoryDomain() == null) {
            return 0L;
        }
        return switch (task.getInventoryDomain()) {
            case VEHICLE -> vehicleScanRepository.countByInventoryTaskId(task.getId());
            case ASSET -> assetScanRepository.countByInventoryTaskId(task.getId());
            case SPARE_PART -> spareScanRepository.countByInventoryTaskId(task.getId());
        };
    }

    /** Must be called after locking and re-validating the task in the caller transaction. */
    @Transactional
    public void purge(InventoryTask task) {
        Long taskId = task.getId();
        List<BackgroundJob> jobs = backgroundJobRepository.findByRelatedIdForUpdate(taskId);
        if (jobs.stream().anyMatch(job -> job.getStatus() == JobStatus.RUNNING)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Task cannot be deleted while its import job is running");
        }

        Set<Long> scanImageIds = new LinkedHashSet<>();
        if (task.getInventoryDomain() == InventoryDomain.VEHICLE) {
            scanImageIds.addAll(vehicleScanRepository.findScanImageIdsByTaskId(taskId));
            vehicleScanRepository.deleteByTaskId(taskId);
            vehicleAssignmentRepository.deleteByTaskId(taskId);
            taskAssignmentRepository.deleteByInventoryTaskId(taskId);
            vehicleItemRepository.deleteByTaskId(taskId);
            vehicleLocationRepository.deleteByTaskId(taskId);
        } else if (task.getInventoryDomain() == InventoryDomain.ASSET) {
            scanImageIds.addAll(assetScanRepository.findScanImageIdsByTaskId(taskId));
            assetScanRepository.deleteByTaskId(taskId);
            assetAssignmentRepository.deleteByTaskId(taskId);
            taskAssignmentRepository.deleteByInventoryTaskId(taskId);
            assetItemRepository.deleteByTaskId(taskId);
            assetPlaceRepository.deleteByTaskId(taskId);
            assetFloorRepository.deleteByTaskId(taskId);
            assetLocationRepository.deleteByTaskId(taskId);
            assetCategoryRepository.deleteByTaskId(taskId);
        } else if (task.getInventoryDomain() == InventoryDomain.SPARE_PART) {
            scanImageIds.addAll(spareScanRepository.findScanImageIdsByTaskId(taskId));
            spareItemRepository.removeScanItemByTaskId(taskId);
            spareScanRepository.removeItemsByTaskId(taskId);

            spareScanRepository.deleteByTaskId(taskId);
            spareAssignmentRepository.deleteByTaskId(taskId);
            taskAssignmentRepository.deleteByInventoryTaskId(taskId);
            spareItemRepository.deleteByTaskId(taskId);
            spareLocationRepository.deleteByTaskId(taskId);
            spareBranchRepository.deleteByTaskId(taskId);
            spareBrandRepository.deleteByTaskId(taskId);
        }

        scanImageIds.forEach(uploadedFileService::markForCleanup);
        backgroundJobRepository.deleteAll(jobs);
        taskRepository.deleteById(taskId);
        taskRepository.flush();
    }
}
