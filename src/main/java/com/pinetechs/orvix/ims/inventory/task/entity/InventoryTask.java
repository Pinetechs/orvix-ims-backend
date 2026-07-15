package com.pinetechs.orvix.ims.inventory.task.entity;

import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "inventory_tasks",
        indexes = {
                @Index(name = "idx_inventory_tasks_company", columnList = "company_id"),
                @Index(name = "idx_inventory_tasks_domain", columnList = "inventory_domain"),
                @Index(name = "idx_inventory_tasks_status", columnList = "status")
        }
)
public class InventoryTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_number", nullable = false, unique = true, length = 100)
    private String taskNumber;


    @Column(name = "task_name", nullable = false, length = 255)
    private String taskName;


    @Column(name = "description", length = 1000)
    private String description;


    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_domain", nullable = false, length = 50)
    private InventoryDomain inventoryDomain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private InventoryTaskStatus status = InventoryTaskStatus.DRAFT;

    @Column(name = "total_records", nullable = false)
    private Integer totalRecords = 0;

    @Column(name = "processed_records", nullable = false)
    private Integer processedRecords = 0;

    @Column(name = "matched_records", nullable = false)
    private Integer matchedRecords = 0;

    @Column(name = "scan_image_required", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean scanImageRequired = true;


    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;


    @Column(name = "pause_reason", length = 500)
    private String pauseReason;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;


    @OneToMany(mappedBy = "inventoryTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InventoryTaskAssignment> assignments = new HashSet<>();

    @Column(name = "notes", length = 1000)
    private String notes;
    private Long importJobId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskNumber() { return taskNumber; }
    public void setTaskNumber(String taskNumber) { this.taskNumber = taskNumber; }
    public InventoryDomain getInventoryDomain() { return inventoryDomain; }
    public void setInventoryDomain(InventoryDomain inventoryDomain) { this.inventoryDomain = inventoryDomain; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public InventoryTaskStatus getStatus() { return status; }
    public void setStatus(InventoryTaskStatus status) { this.status = status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) {

        this.createdBy = createdBy;

    }

    public LocalDateTime getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(LocalDateTime pausedAt) {
        this.pausedAt = pausedAt;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<InventoryTaskAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(Set<InventoryTaskAssignment> assignments) {
        this.assignments = assignments;
    }

    public boolean isScanImageRequired() {
        return scanImageRequired;
    }

    public void setScanImageRequired(boolean scanImageRequired) {
        this.scanImageRequired = scanImageRequired;
    }

    @Transient
    public double getProgressPercentage() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }

        if (processedRecords == null) {
            return 0.0;
        }

        return (processedRecords * 100.0) / totalRecords;
    }


    public boolean isCompleted() {
        return totalRecords != null
                && totalRecords > 0
                && processedRecords != null
                && processedRecords >= totalRecords;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(Integer processedRecords) {
        this.processedRecords = processedRecords;
    }

    public Integer getMatchedRecords() {
        return matchedRecords;
    }

    public void setMatchedRecords(Integer matchedRecords) {
        this.matchedRecords = matchedRecords;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getImportJobId() {
        return importJobId;
    }

    public void setImportJobId(Long importJobId) {
        this.importJobId = importJobId;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
