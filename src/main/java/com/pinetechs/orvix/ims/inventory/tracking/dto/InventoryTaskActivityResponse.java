package com.pinetechs.orvix.ims.inventory.tracking.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskActivity;
import com.pinetechs.orvix.ims.inventory.task.enums.InventoryTaskActivityType;
import com.pinetechs.orvix.ims.user.entity.User;

import java.time.LocalDateTime;

public class InventoryTaskActivityResponse {

    private Long id;
    private InventoryTaskActivityType activityType;
    private InventoryTaskStatus fromStatus;
    private InventoryTaskStatus toStatus;
    private Long performedByUserId;
    private String performedBy;
    private LocalDateTime performedAt;
    private String reason;
    private String details;

    public static InventoryTaskActivityResponse from(InventoryTaskActivity activity) {
        InventoryTaskActivityResponse response = new InventoryTaskActivityResponse();
        response.setId(activity.getId());
        response.setActivityType(activity.getActivityType());
        response.setFromStatus(activity.getFromStatus());
        response.setToStatus(activity.getToStatus());
        response.setPerformedAt(activity.getPerformedAt());
        response.setReason(activity.getReason());
        response.setDetails(activity.getDetails());

        User performedBy = activity.getPerformedBy();
        if (performedBy != null) {
            response.setPerformedByUserId(performedBy.getId());
            String fullName = performedBy.getFullName();
            response.setPerformedBy(fullName == null || fullName.isBlank()
                    ? performedBy.getUsername()
                    : fullName);
        }
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InventoryTaskActivityType getActivityType() { return activityType; }
    public void setActivityType(InventoryTaskActivityType activityType) { this.activityType = activityType; }
    public InventoryTaskStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(InventoryTaskStatus fromStatus) { this.fromStatus = fromStatus; }
    public InventoryTaskStatus getToStatus() { return toStatus; }
    public void setToStatus(InventoryTaskStatus toStatus) { this.toStatus = toStatus; }
    public Long getPerformedByUserId() { return performedByUserId; }
    public void setPerformedByUserId(Long performedByUserId) { this.performedByUserId = performedByUserId; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
