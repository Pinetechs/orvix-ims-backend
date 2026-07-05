package com.pinetechs.orvix.ims.inventory.asset.entity;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "asset_inventory_locations",
        indexes = {
                @Index(name = "idx_asset_inv_locations_task", columnList = "task_id"),
                @Index(name = "idx_asset_inv_locations_name", columnList = "location_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_asset_inv_location_task_name",
                        columnNames = {"task_id", "location_name"}
                )
        }
)
public class AssetInventoryLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @Column(name = "location_name", nullable = false, length = 255)
    private String locationName;

    @Column(name = "total_assets", nullable = false)
    private Integer totalAssets = 0;

    @Column(name = "processed_assets", nullable = false)
    private Integer processedAssets = 0;

    @Column(name = "matched_assets", nullable = false)
    private Integer matchedAssets = 0;

    @Column(name = "missing_assets", nullable = false)
    private Integer missingAssets = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public double getProgressPercentage() {
        if (totalAssets == null || totalAssets == 0) {
            return 0;
        }
        return (processedAssets * 100.0) / totalAssets;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InventoryTask getInventoryTask() { return inventoryTask; }
    public void setInventoryTask(InventoryTask inventoryTask) { this.inventoryTask = inventoryTask; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public Integer getTotalAssets() { return totalAssets; }
    public void setTotalAssets(Integer totalAssets) { this.totalAssets = totalAssets; }

    public Integer getProcessedAssets() { return processedAssets; }
    public void setProcessedAssets(Integer processedAssets) { this.processedAssets = processedAssets; }

    public Integer getMatchedAssets() { return matchedAssets; }
    public void setMatchedAssets(Integer matchedAssets) { this.matchedAssets = matchedAssets; }

    public Integer getMissingAssets() { return missingAssets; }
    public void setMissingAssets(Integer missingAssets) { this.missingAssets = missingAssets; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
