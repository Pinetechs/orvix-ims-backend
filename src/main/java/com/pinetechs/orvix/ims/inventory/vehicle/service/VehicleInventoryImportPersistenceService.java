package com.pinetechs.orvix.ims.inventory.vehicle.service;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocation;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class VehicleInventoryImportPersistenceService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final VehicleInventoryItemRepository itemRepository;
    private final VehicleInventoryLocationRepository locationRepository;

    public VehicleInventoryImportPersistenceService(
            InventoryTaskRepository inventoryTaskRepository,
            VehicleInventoryItemRepository itemRepository,
            VehicleInventoryLocationRepository locationRepository
    ) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
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

    @Transactional
    public void replaceVehicleInventoryData(
            Long taskId,
            List<VehicleInventoryItem> items,
            Collection<VehicleInventoryLocation> locations
    ) {
        InventoryTask task = inventoryTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Inventory task not found. taskId=" + taskId));

        for (VehicleInventoryItem item : items) {
            item.setInventoryTask(task);
        }

        for (VehicleInventoryLocation location : locations) {
            location.setInventoryTask(task);
        }

        itemRepository.deleteByTaskId(taskId);
        locationRepository.deleteByTaskId(taskId);

        itemRepository.saveAll(items);
        locationRepository.saveAll(locations);

        task.setTotalRecords(items.size());
        task.setProcessedRecords(0);
        task.setMatchedRecords(0);

        // حسب المطلوب: بعد انتهاء معالجة البيانات ترجع إلى DRAFT
        task.setStatus(InventoryTaskStatus.DRAFT);

        inventoryTaskRepository.save(task);
    }
}
