package com.pinetechs.orvix.ims.inventory.vehicle.service.impl;

import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.service.VehicleInventoryQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class VehicleInventoryQueryServiceImpl implements VehicleInventoryQueryService {

    private final InventoryTaskRepository taskRepository;
    private final VehicleInventoryItemRepository itemRepository;

    public VehicleInventoryQueryServiceImpl(
            InventoryTaskRepository taskRepository,
            VehicleInventoryItemRepository itemRepository
    ) {
        this.taskRepository = taskRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public Page<VehicleInventoryItem> getTaskItems(Long taskId, Pageable pageable) {
        return null;
    }
}