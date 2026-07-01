package com.pinetechs.orvix.ims.inventory.vehicle.service.impl;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryItemResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.service.VehicleInventoryQueryService;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class VehicleInventoryQueryServiceImpl implements VehicleInventoryQueryService {

    private final InventoryTaskRepository taskRepository;
    private final VehicleInventoryItemRepository itemRepository;
    private final AccessPolicyService accessPolicyService;

    public VehicleInventoryQueryServiceImpl(
            InventoryTaskRepository taskRepository,
            VehicleInventoryItemRepository itemRepository,
            AccessPolicyService accessPolicyService) {
        this.taskRepository = taskRepository;
        this.itemRepository = itemRepository;
        this.accessPolicyService = accessPolicyService;
    }

    @Override
    public Page<VehicleInventoryItemResponse> getTaskItems(Long taskId, User currentUser, Pageable pageable) {
        InventoryTask task = taskRepository.findById(taskId).orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,"Inventory task not found"));
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);

        return itemRepository.findByInventoryTask_Id(taskId, pageable).map(item -> VehicleInventoryItemResponse.from(item));
    }
}