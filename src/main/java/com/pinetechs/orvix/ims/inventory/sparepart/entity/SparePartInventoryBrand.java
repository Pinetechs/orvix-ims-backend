package com.pinetechs.orvix.ims.inventory.sparepart.entity;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "spare_part_inventory_brands",
        indexes = {
                @Index(name = "idx_sp_inv_brands_task", columnList = "task_id"),
                @Index(name = "idx_sp_inv_brands_name", columnList = "brand_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sp_inv_brand_task_name",
                        columnNames = {"task_id", "brand_name"}
                )
        }
)
public class SparePartInventoryBrand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @Column(name = "brand_name", nullable = false, length = 255)
    private String brandName;

    @Column(name = "total_items", nullable = false)
    private Integer totalItems = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InventoryTask getInventoryTask() { return inventoryTask; }
    public void setInventoryTask(InventoryTask inventoryTask) { this.inventoryTask = inventoryTask; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
