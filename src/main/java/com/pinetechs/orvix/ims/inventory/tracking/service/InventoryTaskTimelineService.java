package com.pinetechs.orvix.ims.inventory.tracking.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskActivityRepository;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.tracking.dto.InventoryTaskActivityResponse;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryTaskTimelineService {

    private final InventoryTaskRepository taskRepository;
    private final InventoryTaskActivityRepository activityRepository;
    private final AccessPolicyService accessPolicyService;

    public InventoryTaskTimelineService(
            InventoryTaskRepository taskRepository,
            InventoryTaskActivityRepository activityRepository,
            AccessPolicyService accessPolicyService
    ) {
        this.taskRepository = taskRepository;
        this.activityRepository = activityRepository;
        this.accessPolicyService = accessPolicyService;
    }

    @Transactional(readOnly = true)
    public Page<InventoryTaskActivityResponse> getTimeline(
            Long taskId,
            Pageable pageable,
            User currentUser
    ) {
        InventoryTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Inventory task not found"));
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);

        return activityRepository
                .findByInventoryTaskIdOrderByPerformedAtDescIdDesc(taskId, pageable)
                .map(InventoryTaskActivityResponse::from);
    }
}
