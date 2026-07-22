package com.pinetechs.orvix.ims.inventory.tracking.policy;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaLevel;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TrackingStatusPolicy {

    public static final long STALLED_AFTER_MINUTES = 30;
    public static final int DUPLICATE_RATE_MINIMUM_EVENTS = 20;
    public static final double HIGH_DUPLICATE_RATE_PERCENTAGE = 20.0;

    public TrackingAreaStatus areaStatus(long planned, long processed, long scanEvents, LocalDateTime lastActivity, InventoryTaskStatus taskStatus) {
        if (planned > 0 && processed >= planned) {
            return TrackingAreaStatus.COMPLETED;
        }
        if (processed == 0 && scanEvents == 0) {
            return TrackingAreaStatus.NOT_STARTED;
        }
        if (taskStatus == InventoryTaskStatus.IN_PROGRESS && isInactive(lastActivity)) {
            return TrackingAreaStatus.STALLED;
        }
        return TrackingAreaStatus.ACTIVE;
    }

    public boolean isTaskStalled(InventoryTask task, TrackingResponses.EventMetrics events) {
        if (task.getStatus() != InventoryTaskStatus.IN_PROGRESS) {
            return false;
        }
        LocalDateTime reference = events.lastActivityAt() == null
                ? task.getStartedAt()
                : events.lastActivityAt();
        return isInactive(reference);
    }

    public boolean hasBeenRunningLongEnough(InventoryTask task) {
        return task.getStatus() == InventoryTaskStatus.IN_PROGRESS
                && task.getStartedAt() != null
                && isInactive(task.getStartedAt());
    }

    public boolean isReadyForReview(InventoryTask task, TrackingResponses.CurrentMetrics current) {
        return task.getStatus() == InventoryTaskStatus.IN_PROGRESS
                && current.totalExpected() > 0
                && current.remaining() == 0;
    }

    public boolean isHighDuplicateRate(TrackingResponses.EventMetrics events) {
        return events.totalEvents() >= DUPLICATE_RATE_MINIMUM_EVENTS
                && events.duplicates() * 100.0 / events.totalEvents()
                >= HIGH_DUPLICATE_RATE_PERCENTAGE;
    }

    public int duplicateRate(TrackingResponses.EventMetrics events) {
        if (events.totalEvents() == 0) {
            return 0;
        }
        return (int) Math.round(events.duplicates() * 100.0 / events.totalEvents());
    }

    public boolean isLeafArea(InventoryDomain domain, TrackingResponses.Area area) {
        return switch (domain) {
            case VEHICLE -> area.level() == TrackingAreaLevel.STORE;
            case ASSET -> area.level() == TrackingAreaLevel.PLACE;
            case SPARE_PART -> area.level() == TrackingAreaLevel.BRANCH;
        };
    }

    private boolean isInactive(LocalDateTime activityTime) {
        return activityTime != null
                && activityTime.isBefore(LocalDateTime.now().minusMinutes(STALLED_AFTER_MINUTES));
    }
}
