package com.pinetechs.orvix.ims.inventory.sparepart.entity;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "spare_part_inventory_branch_assignments",
        indexes = {
                @Index(name = "idx_sp_inv_branch_assign_assignment", columnList = "assignment_id"),
                @Index(name = "idx_sp_inv_branch_assign_branch", columnList = "branch_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sp_inv_branch_assign_assignment_branch",
                        columnNames = {"assignment_id", "branch_id"}
                )
        }
)
public class SparePartInventoryBranchAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private InventoryTaskAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private SparePartInventoryBranch branch;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InventoryTaskAssignment getAssignment() { return assignment; }
    public void setAssignment(InventoryTaskAssignment assignment) { this.assignment = assignment; }
    public SparePartInventoryBranch getBranch() { return branch; }
    public void setBranch(SparePartInventoryBranch branch) { this.branch = branch; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
