package com.pinetechs.orvix.ims.inventory.task.service;

import com.pinetechs.orvix.ims.inventory.task.dto.CreateInventoryTaskRequest;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;


public interface InventoryTaskService {

        InventoryTask createTask(CreateInventoryTaskRequest createInventoryTaskRequest, User currentUser);

        InventoryTask startTask(Long taskId);

        InventoryTask pauseTask(Long taskId, String pauseReason);

        InventoryTask resumeTask(Long taskId);

        InventoryTask moveToReview(Long taskId);

        InventoryTask completeTask(Long taskId);

        InventoryTask cancelTask(Long taskId, String cancelReason);
    }


