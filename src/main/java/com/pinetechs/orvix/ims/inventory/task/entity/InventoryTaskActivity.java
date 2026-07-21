package com.pinetechs.orvix.ims.inventory.task.entity;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.enums.InventoryTaskActivityType;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory_task_activities",
        indexes = {
                @Index(name = "idx_inventory_task_activity_task_time", columnList = "task_id, performed_at, id"),
                @Index(name = "idx_inventory_task_activity_type", columnList = "activity_type")
        }
)
public class InventoryTaskActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private InventoryTaskActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 50)
    private InventoryTaskStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 50)
    private InventoryTaskStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id")
    private User performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "details", length = 2000)
    private String details;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InventoryTask getInventoryTask() { return inventoryTask; }
    public void setInventoryTask(InventoryTask inventoryTask) { this.inventoryTask = inventoryTask; }
    public InventoryTaskActivityType getActivityType() { return activityType; }
    public void setActivityType(InventoryTaskActivityType activityType) { this.activityType = activityType; }
    public InventoryTaskStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(InventoryTaskStatus fromStatus) { this.fromStatus = fromStatus; }
    public InventoryTaskStatus getToStatus() { return toStatus; }
    public void setToStatus(InventoryTaskStatus toStatus) { this.toStatus = toStatus; }
    public User getPerformedBy() { return performedBy; }
    public void setPerformedBy(User performedBy) { this.performedBy = performedBy; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
