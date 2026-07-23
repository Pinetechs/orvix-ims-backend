package com.pinetechs.orvix.ims.inventory.review.entity;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckRequestStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "inventory_recheck_requests",
        indexes = {
                @Index(name = "idx_recheck_request_task_status", columnList = "task_id, status"),
                @Index(name = "idx_recheck_request_user_status", columnList = "assigned_to_user_id, status")
        }
)
public class InventoryRecheckRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number", nullable = false, unique = true, length = 80)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_domain", nullable = false, length = 30)
    private InventoryDomain inventoryDomain;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_to_user_id", nullable = false)
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private RecheckRequestStatus status = RecheckRequestStatus.PENDING;

    @Column(name = "work_area_key", nullable = false, length = 300)
    private String workAreaKey;

    @Column(name = "work_area_label", length = 500)
    private String workAreaLabel;

    @Column(name = "instructions", length = 1500)
    private String instructions;

    @Column(name = "image_required", nullable = false)
    private boolean imageRequired;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "recheckRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<InventoryRecheckItem> items = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addItem(InventoryRecheckItem item) {
        item.setRecheckRequest(this);
        items.add(item);
    }

    public void start(LocalDateTime time) {
        if (startedAt == null) startedAt = time;
        status = RecheckRequestStatus.IN_PROGRESS;
    }

    public void markSubmitted(LocalDateTime time) {
        status = RecheckRequestStatus.SUBMITTED;
        submittedAt = time;
    }

    public void complete(LocalDateTime time) {
        status = RecheckRequestStatus.COMPLETED;
        completedAt = time;
    }

    public void cancel(String reason, LocalDateTime time) {
        status = RecheckRequestStatus.CANCELLED;
        cancellationReason = reason;
        cancelledAt = time;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestNumber() { return requestNumber; }
    public void setRequestNumber(String requestNumber) { this.requestNumber = requestNumber; }
    public InventoryTask getInventoryTask() { return inventoryTask; }
    public void setInventoryTask(InventoryTask inventoryTask) { this.inventoryTask = inventoryTask; }
    public InventoryDomain getInventoryDomain() { return inventoryDomain; }
    public void setInventoryDomain(InventoryDomain inventoryDomain) { this.inventoryDomain = inventoryDomain; }
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }
    public RecheckRequestStatus getStatus() { return status; }
    public void setStatus(RecheckRequestStatus status) { this.status = status; }
    public String getWorkAreaKey() { return workAreaKey; }
    public void setWorkAreaKey(String workAreaKey) { this.workAreaKey = workAreaKey; }
    public String getWorkAreaLabel() { return workAreaLabel; }
    public void setWorkAreaLabel(String workAreaLabel) { this.workAreaLabel = workAreaLabel; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public boolean isImageRequired() { return imageRequired; }
    public void setImageRequired(boolean imageRequired) { this.imageRequired = imageRequired; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
    public List<InventoryRecheckItem> getItems() { return items; }
    public void setItems(List<InventoryRecheckItem> items) { this.items = items; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
