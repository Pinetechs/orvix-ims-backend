package com.pinetechs.orvix.ims.inventory.task.service;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskActivity;
import com.pinetechs.orvix.ims.inventory.task.enums.InventoryTaskActivityType;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskActivityRepository;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class InventoryTaskActivityService {

    private final InventoryTaskActivityRepository activityRepository;
    private final InventoryTaskRepository taskRepository;

    public InventoryTaskActivityService(
            InventoryTaskActivityRepository activityRepository,
            InventoryTaskRepository taskRepository
    ) {
        this.activityRepository = activityRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public void record(
            InventoryTask task,
            InventoryTaskActivityType activityType,
            InventoryTaskStatus fromStatus,
            InventoryTaskStatus toStatus,
            User performedBy,
            String reason,
            String details
    ) {
        InventoryTaskActivity activity = new InventoryTaskActivity();
        activity.setInventoryTask(task);
        activity.setActivityType(activityType);
        activity.setFromStatus(fromStatus);
        activity.setToStatus(toStatus);
        activity.setPerformedBy(performedBy);
        activity.setPerformedAt(LocalDateTime.now());
        activity.setReason(trimToLength(reason, 500));
        activity.setDetails(trimToLength(details, 2000));
        activityRepository.save(activity);
    }

    /**
     * Atomically starts a READY_TO_START task on its first accepted scan.
     * Only the transaction that changes the status records the activity.
     */
    @Transactional
    public boolean startOnFirstScan(InventoryTask task, User performedBy) {
        LocalDateTime startedAt = LocalDateTime.now();
        int updated = taskRepository.markInProgressOnFirstScan(
                task.getId(),
                startedAt.toLocalDate(),
                startedAt,
                InventoryTaskStatus.READY_TO_START,
                InventoryTaskStatus.IN_PROGRESS
        );

        if (updated == 1) {
            record(
                    task,
                    InventoryTaskActivityType.TASK_STARTED_BY_FIRST_SCAN,
                    InventoryTaskStatus.READY_TO_START,
                    InventoryTaskStatus.IN_PROGRESS,
                    performedBy,
                    null,
                    "Task started automatically by the first accepted scan"
            );
            return true;
        }
        return false;
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}
