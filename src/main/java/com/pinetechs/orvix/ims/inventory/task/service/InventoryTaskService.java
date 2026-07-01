package com.pinetechs.orvix.ims.inventory.task.service;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.dto.CreateInventoryTaskRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.TaskResponse;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface InventoryTaskService {

        InventoryTask createTask(CreateInventoryTaskRequest createInventoryTaskRequest, User currentUser);

        InventoryTask startTask(Long taskId);

        InventoryTask pauseTask(Long taskId, String pauseReason);

        InventoryTask resumeTask(Long taskId);

        InventoryTask moveToReview(Long taskId);

        InventoryTask completeTask(Long taskId);

        InventoryTask cancelTask(Long taskId, String cancelReason);
        TaskResponse getTaskById(Long taskId ,User currentUser);

        Page<TaskResponse> getTasks(Pageable pageable, User currentUser, String search,Long companyId, InventoryTaskStatus status, String inventoryDomains);
}


