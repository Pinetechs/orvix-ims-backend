package com.pinetechs.orvix.ims.inventory.task.entity;

import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.inventory.enums.InventoryDomain;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_domain", nullable = false, length = 50)
    private InventoryDomain inventoryDomain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private InventoryTaskStatus status = InventoryTaskStatus.DRAFT;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "inventory_task_users",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignedUsers = new HashSet<>();

    @Column(name = "notes", length = 1000)
    private String notes;

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
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public Set<User> getAssignedUsers() { return assignedUsers; }
    public void setAssignedUsers(Set<User> assignedUsers) { this.assignedUsers = assignedUsers; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
