package com.pinetechs.orvix.ims.inventory.tracking.service;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskActivity;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrackingDurationService {

    private final InventoryTaskActivityRepository activityRepository;

    public TrackingDurationService(InventoryTaskActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Transactional(readOnly = true)
    public long activeWorkingSeconds(InventoryTask task) {
        List<InventoryTaskActivity> activities = activityRepository
                .findByInventoryTaskIdOrderByPerformedAtAscIdAsc(task.getId());

        LocalDateTime activeFrom = null;
        long seconds = 0;
        for (InventoryTaskActivity activity : activities) {
            if (activity.getToStatus() == InventoryTaskStatus.IN_PROGRESS
                    && activity.getFromStatus() != InventoryTaskStatus.IN_PROGRESS) {
                activeFrom = activity.getPerformedAt();
            }
            if (activeFrom != null
                    && activity.getFromStatus() == InventoryTaskStatus.IN_PROGRESS
                    && activity.getToStatus() != InventoryTaskStatus.IN_PROGRESS) {
                seconds += positiveSeconds(activeFrom, activity.getPerformedAt());
                activeFrom = null;
            }
        }

        if (activeFrom != null && task.getStatus() == InventoryTaskStatus.IN_PROGRESS) {
            seconds += positiveSeconds(activeFrom, LocalDateTime.now());
        }

        if (seconds == 0 && task.getStartedAt() != null && activities.isEmpty()) {
            LocalDateTime end = task.getStatus() == InventoryTaskStatus.IN_PROGRESS
                    ? LocalDateTime.now()
                    : firstNonNull(task.getPausedAt(), task.getReviewStartedAt(), task.getClosedAt());
            if (end != null) seconds = positiveSeconds(task.getStartedAt(), end);
        }
        return seconds;
    }

    private long positiveSeconds(LocalDateTime start, LocalDateTime end) {
        return Math.max(Duration.between(start, end).getSeconds(), 0);
    }

    private LocalDateTime firstNonNull(LocalDateTime... values) {
        for (LocalDateTime value : values) if (value != null) return value;
        return null;
    }
}
