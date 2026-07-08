package com.pinetechs.orvix.ims.inventory.sparepart.service;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.sparepart.entity.*;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.*;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class SparePartInventoryImportPersistenceService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final SparePartInventoryItemRepository itemRepository;
    private final SparePartInventoryItemJdbcRepository itemJdbcRepository;
    private final SparePartInventoryBranchRepository branchRepository;
    private final SparePartInventoryLocationRepository locationRepository;
    private final SparePartInventoryBrandRepository brandRepository;
    private final SparePartInventoryBranchAssignmentRepository branchAssignmentRepository;
    private final SparePartInventoryScanRepository scanRepository;
    private final InventoryTaskAssignmentRepository assignmentRepository;

    public SparePartInventoryImportPersistenceService(
            InventoryTaskRepository inventoryTaskRepository,
            SparePartInventoryItemRepository itemRepository,
            SparePartInventoryItemJdbcRepository itemJdbcRepository,
            SparePartInventoryBranchRepository branchRepository,
            SparePartInventoryLocationRepository locationRepository,
            SparePartInventoryBrandRepository brandRepository,
            SparePartInventoryBranchAssignmentRepository branchAssignmentRepository,
            SparePartInventoryScanRepository scanRepository,
            InventoryTaskAssignmentRepository assignmentRepository
    ) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.itemRepository = itemRepository;
        this.itemJdbcRepository = itemJdbcRepository;
        this.branchRepository = branchRepository;
        this.locationRepository = locationRepository;
        this.brandRepository = brandRepository;
        this.branchAssignmentRepository = branchAssignmentRepository;
        this.scanRepository = scanRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public InventoryTask markImportInProgress(Long taskId) {
        InventoryTask task = inventoryTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Inventory task not found. taskId=" + taskId));
        task.setStatus(InventoryTaskStatus.IMPORT_IN_PROGRESS);
        return inventoryTaskRepository.save(task);
    }

    @Transactional
    public void markImportFailed(Long taskId) {
        if (taskId == null) {
            return;
        }
        inventoryTaskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(InventoryTaskStatus.IMPORT_FAILED);
            inventoryTaskRepository.save(task);
        });
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void replaceSparePartInventoryData(
            Long taskId,
            List<SparePartInventoryItem> items,
            Collection<SparePartInventoryBranch> branches,
            Collection<SparePartInventoryLocation> locations,
            Collection<SparePartInventoryBrand> brands
    ) {
        InventoryTask task = inventoryTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new RuntimeException("Inventory task not found. taskId=" + taskId));

        for (SparePartInventoryBranch branch : branches) {
            branch.setInventoryTask(task);
        }
        for (SparePartInventoryLocation location : locations) {
            location.setInventoryTask(task);
        }
        for (SparePartInventoryBrand brand : brands) {
            brand.setInventoryTask(task);
        }
        for (SparePartInventoryItem item : items) {
            item.setInventoryTask(task);
        }

        branchAssignmentRepository.deleteByTaskId(taskId);
        assignmentRepository.deleteByInventoryTaskId(taskId);
        scanRepository.deleteByTaskId(taskId);
        itemRepository.deleteByTaskId(taskId);
        locationRepository.deleteByTaskId(taskId);
        branchRepository.deleteByTaskId(taskId);
        brandRepository.deleteByTaskId(taskId);

        branchRepository.saveAllAndFlush(List.copyOf(branches));
        locationRepository.saveAllAndFlush(List.copyOf(locations));
        brandRepository.saveAllAndFlush(List.copyOf(brands));

        itemJdbcRepository.batchInsert(taskId, items);

        task.setTotalRecords(items.size());
        task.setProcessedRecords(0);
        task.setMatchedRecords(0);
        task.setStatus(InventoryTaskStatus.IMPORT_COMPLETED);

        inventoryTaskRepository.save(task);
    }
}
