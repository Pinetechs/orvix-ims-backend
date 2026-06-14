package com.pinetechs.orvix.ims.inventory.task.service;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;


    public interface InventoryTaskService {

        InventoryTask createVehicleTask(Long companyId, Long createdByUserId, String notes);

        InventoryTask startTask(Long taskId);

        InventoryTask pauseTask(Long taskId, String pauseReason);

        InventoryTask resumeTask(Long taskId);

        InventoryTask moveToReview(Long taskId);

        InventoryTask completeTask(Long taskId);

        InventoryTask cancelTask(Long taskId, String cancelReason);
    }


