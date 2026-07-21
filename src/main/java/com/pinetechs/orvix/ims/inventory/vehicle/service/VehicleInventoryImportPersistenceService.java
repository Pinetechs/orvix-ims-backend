package com.pinetechs.orvix.ims.inventory.vehicle.service;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.task.enums.InventoryTaskActivityType;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskActivityService;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocation;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryItemJdbcRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class VehicleInventoryImportPersistenceService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final VehicleInventoryItemRepository itemRepository;
    private final VehicleInventoryLocationRepository locationRepository;
    private final VehicleInventoryItemJdbcRepository itemJdbcRepository;
    private final InventoryTaskAssignmentRepository assignmentRepository;
    private final VehicleInventoryLocationAssignmentRepository locationAssignmentRepository;
    private final InventoryTaskActivityService taskActivityService;

    public VehicleInventoryImportPersistenceService(InventoryTaskRepository inventoryTaskRepository,
                                                    VehicleInventoryItemRepository itemRepository,
                                                    VehicleInventoryLocationRepository locationRepository,
                                                    VehicleInventoryItemJdbcRepository itemJdbcRepository,
                                                    InventoryTaskAssignmentRepository assignmentRepository,
                                                    VehicleInventoryLocationAssignmentRepository locationAssignmentRepository,
                                                    InventoryTaskActivityService taskActivityService) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.itemJdbcRepository = itemJdbcRepository;
        this.assignmentRepository = assignmentRepository;
        this.locationAssignmentRepository = locationAssignmentRepository;
        this.taskActivityService = taskActivityService;
    }

    @Transactional
    public InventoryTask markImportInProgress(Long taskId) {
        InventoryTask task = inventoryTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Inventory task not found. taskId=" + taskId));

        InventoryTaskStatus fromStatus = task.getStatus();
        task.setStatus(InventoryTaskStatus.IMPORT_IN_PROGRESS);

        InventoryTask savedTask = inventoryTaskRepository.save(task);
        taskActivityService.record(savedTask, InventoryTaskActivityType.IMPORT_STARTED,
                fromStatus, InventoryTaskStatus.IMPORT_IN_PROGRESS, null, null,
                "jobId=" + savedTask.getImportJobId());
        return savedTask;
    }

    @Transactional
    public void markImportFailed(Long taskId) {
        if (taskId == null) {
            return;
        }

        inventoryTaskRepository.findById(taskId).ifPresent(task -> {
            InventoryTaskStatus fromStatus = task.getStatus();
            task.setStatus(InventoryTaskStatus.IMPORT_FAILED);
            inventoryTaskRepository.save(task);
            if (fromStatus != InventoryTaskStatus.IMPORT_FAILED) {
                taskActivityService.record(task, InventoryTaskActivityType.IMPORT_FAILED,
                        fromStatus, InventoryTaskStatus.IMPORT_FAILED, null, null,
                        "jobId=" + task.getImportJobId());
            }
        });
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void replaceVehicleInventoryData(Long taskId, List<VehicleInventoryItem> items, Collection<VehicleInventoryLocation> locations) {
        InventoryTask task = inventoryTaskRepository.findByIdForUpdate(taskId).orElseThrow(() -> new RuntimeException("Inventory task not found. taskId=" + taskId));

        for (VehicleInventoryItem item : items) {
            item.setInventoryTask(task);
        }

        for (VehicleInventoryLocation location : locations) {
            location.setInventoryTask(task);
        }

        locationAssignmentRepository.deleteByTaskId(taskId);
        assignmentRepository.deleteByInventoryTaskId(taskId);
        itemRepository.deleteByTaskId(taskId);
        locationRepository.deleteByTaskId(taskId);

        itemJdbcRepository.batchInsert(taskId,items);
        locationRepository.saveAll(locations);

        task.setTotalRecords(items.size());
        task.setProcessedRecords(0);
        task.setMatchedRecords(0);

        InventoryTaskStatus fromStatus = task.getStatus();
        task.setStatus(InventoryTaskStatus.IMPORT_COMPLETED);

        inventoryTaskRepository.save(task);
        taskActivityService.record(task, InventoryTaskActivityType.IMPORT_COMPLETED,
                fromStatus, InventoryTaskStatus.IMPORT_COMPLETED, null, null,
                "jobId=" + task.getImportJobId() + ", records=" + items.size()
                        + ", locations=" + locations.size());
    }
}
