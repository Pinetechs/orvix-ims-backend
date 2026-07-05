package com.pinetechs.orvix.ims.inventory.asset.service;

import com.pinetechs.orvix.ims.inventory.asset.entity.*;
import com.pinetechs.orvix.ims.inventory.asset.repository.*;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class AssetInventoryImportPersistenceService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final InventoryTaskAssignmentRepository assignmentRepository;
    private final AssetInventoryItemRepository itemRepository;
    private final AssetInventoryItemJdbcRepository itemJdbcRepository;
    private final AssetInventoryLocationRepository locationRepository;
    private final AssetInventoryFloorRepository floorRepository;
    private final AssetInventoryPlaceRepository placeRepository;
    private final AssetInventoryCategoryRepository categoryRepository;
    private final AssetInventoryLocationAssignmentRepository locationAssignmentRepository;
    private final AssetInventoryScanRepository scanRepository;

    public AssetInventoryImportPersistenceService(
            InventoryTaskRepository inventoryTaskRepository,
            InventoryTaskAssignmentRepository assignmentRepository,
            AssetInventoryItemRepository itemRepository,
            AssetInventoryItemJdbcRepository itemJdbcRepository,
            AssetInventoryLocationRepository locationRepository,
            AssetInventoryFloorRepository floorRepository,
            AssetInventoryPlaceRepository placeRepository,
            AssetInventoryCategoryRepository categoryRepository,
            AssetInventoryLocationAssignmentRepository locationAssignmentRepository,
            AssetInventoryScanRepository scanRepository
    ) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.assignmentRepository = assignmentRepository;
        this.itemRepository = itemRepository;
        this.itemJdbcRepository = itemJdbcRepository;
        this.locationRepository = locationRepository;
        this.floorRepository = floorRepository;
        this.placeRepository = placeRepository;
        this.categoryRepository = categoryRepository;
        this.locationAssignmentRepository = locationAssignmentRepository;
        this.scanRepository = scanRepository;
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
    public void replaceAssetInventoryData(
            Long taskId,
            List<AssetInventoryItem> items,
            Collection<AssetInventoryLocation> locations,
            Collection<AssetInventoryFloor> floors,
            Collection<AssetInventoryPlace> places,
            Collection<AssetInventoryCategory> categories
    ) {
        InventoryTask task = inventoryTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new RuntimeException("Inventory task not found. taskId=" + taskId));

        for (AssetInventoryLocation location : locations) {
            location.setInventoryTask(task);
        }

        for (AssetInventoryFloor floor : floors) {
            floor.setInventoryTask(task);
        }

        for (AssetInventoryPlace place : places) {
            place.setInventoryTask(task);
        }

        for (AssetInventoryCategory category : categories) {
            category.setInventoryTask(task);
        }

        for (AssetInventoryItem item : items) {
            item.setInventoryTask(task);
        }

        locationAssignmentRepository.deleteByTaskId(taskId);
        assignmentRepository.deleteByInventoryTaskId(taskId);
        scanRepository.deleteByTaskId(taskId);
        itemRepository.deleteByTaskId(taskId);
        placeRepository.deleteByTaskId(taskId);
        floorRepository.deleteByTaskId(taskId);
        locationRepository.deleteByTaskId(taskId);
        categoryRepository.deleteByTaskId(taskId);

        locationRepository.saveAllAndFlush(List.copyOf(locations));
        floorRepository.saveAllAndFlush(List.copyOf(floors));
        placeRepository.saveAllAndFlush(List.copyOf(places));
        categoryRepository.saveAllAndFlush(List.copyOf(categories));

        itemJdbcRepository.batchInsert(taskId, items);

        task.setTotalRecords(items.size());
        task.setProcessedRecords(0);
        task.setMatchedRecords(0);
        task.setStatus(InventoryTaskStatus.IMPORT_COMPLETED);

        inventoryTaskRepository.save(task);
    }
}
