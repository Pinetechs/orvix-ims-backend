package com.pinetechs.orvix.ims.inventory.sparepart.entity;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "spare_part_inventory_branches",
        indexes = {
                @Index(name = "idx_sp_inv_branches_task", columnList = "task_id"),
                @Index(name = "idx_sp_inv_branches_name", columnList = "branch_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sp_inv_branch_task_name",
                        columnNames = {"task_id", "branch_name"}
                )
        }
)
public class SparePartInventoryBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @Column(name = "branch_name", nullable = false, length = 255)
    private String branchName;

    @Column(name = "total_items", nullable = false)
    private Integer totalItems = 0;

    @Column(name = "counted_items", nullable = false)
    private Integer countedItems = 0;

    @Column(name = "matched_items", nullable = false)
    private Integer matchedItems = 0;

    @Column(name = "shortage_items", nullable = false)
    private Integer shortageItems = 0;

    @Column(name = "overage_items", nullable = false)
    private Integer overageItems = 0;

    @Column(name = "location_mismatch_items", nullable = false)
    private Integer locationMismatchItems = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public double getProgressPercentage() {
        if (totalItems == null || totalItems == 0) {
            return 0;
        }
        return (countedItems == null ? 0 : countedItems) * 100.0 / totalItems;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InventoryTask getInventoryTask() { return inventoryTask; }
    public void setInventoryTask(InventoryTask inventoryTask) { this.inventoryTask = inventoryTask; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
    public Integer getCountedItems() { return countedItems; }
    public void setCountedItems(Integer countedItems) { this.countedItems = countedItems; }
    public Integer getMatchedItems() { return matchedItems; }
    public void setMatchedItems(Integer matchedItems) { this.matchedItems = matchedItems; }
    public Integer getShortageItems() { return shortageItems; }
    public void setShortageItems(Integer shortageItems) { this.shortageItems = shortageItems; }
    public Integer getOverageItems() { return overageItems; }
    public void setOverageItems(Integer overageItems) { this.overageItems = overageItems; }
    public Integer getLocationMismatchItems() { return locationMismatchItems; }
    public void setLocationMismatchItems(Integer locationMismatchItems) { this.locationMismatchItems = locationMismatchItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
