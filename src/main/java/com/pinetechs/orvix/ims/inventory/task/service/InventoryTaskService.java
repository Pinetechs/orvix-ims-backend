package com.pinetechs.orvix.ims.inventory.task.service;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.dto.CreateInventoryTaskRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.TaskResponse;
import com.pinetechs.orvix.ims.inventory.task.dto.UpdateSparePartLocationProgressModeRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.UpdateInventoryTaskScanSettingsRequest;
import com.pinetechs.orvix.ims.user.dto.UserResponse;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface InventoryTaskService {

        TaskResponse createTask(CreateInventoryTaskRequest createInventoryTaskRequest, User currentUser);

        TaskResponse startTask(Long taskId, User currentUser);

        TaskResponse markReadyToStart(Long taskId, User currentUser);

        TaskResponse pauseTask(Long taskId, String pauseReason, User currentUser);

        TaskResponse resumeTask(Long taskId, User currentUser);

        TaskResponse moveToReview(Long taskId, User currentUser);

        TaskResponse completeTask(Long taskId, User currentUser);

        TaskResponse cancelTask(Long taskId, String cancelReason, User currentUser);
        TaskResponse getTaskById(Long taskId ,User currentUser);

        TaskResponse updateSparePartLocationProgressMode(
                Long taskId,
                UpdateSparePartLocationProgressModeRequest request,
                User currentUser
        );

        TaskResponse updateScanSettings(Long taskId, UpdateInventoryTaskScanSettingsRequest request, User currentUser);

        Page<UserResponse> getEligibleStaff(Long taskId, String search, Pageable pageable, User currentUser);

        void deleteTask(Long taskId, User currentUser);

        Page<TaskResponse> getTasks(Pageable pageable, User currentUser, String search,Long companyId, InventoryTaskStatus status, String inventoryDomains);
}
