package com.pinetechs.orvix.ims.inventory.asset.entity;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "asset_inventory_floors",
        indexes = {
                @Index(name = "idx_asset_inv_floors_task", columnList = "task_id"),
                @Index(name = "idx_asset_inv_floors_location", columnList = "location_id"),
                @Index(name = "idx_asset_inv_floors_name", columnList = "floor_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_asset_inv_floor_task_location_name",
                        columnNames = {"task_id", "location_id", "floor_name"}
                )
        }
)
public class AssetInventoryFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private AssetInventoryLocation location;

    @Column(name = "floor_name", nullable = false, length = 255)
    private String floorName;

    @Column(name = "total_assets", nullable = false)
    private Integer totalAssets = 0;

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

    public AssetInventoryLocation getLocation() { return location; }
    public void setLocation(AssetInventoryLocation location) { this.location = location; }

    public String getFloorName() { return floorName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }

    public Integer getTotalAssets() { return totalAssets; }
    public void setTotalAssets(Integer totalAssets) { this.totalAssets = totalAssets; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
