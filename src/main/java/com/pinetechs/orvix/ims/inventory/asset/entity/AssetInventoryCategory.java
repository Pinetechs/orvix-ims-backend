package com.pinetechs.orvix.ims.inventory.asset.entity;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "asset_inventory_categories",
        indexes = {
                @Index(name = "idx_asset_inv_categories_task", columnList = "task_id"),
                @Index(name = "idx_asset_inv_categories_category", columnList = "asset_category"),
                @Index(name = "idx_asset_inv_categories_type", columnList = "asset_type"),
                @Index(name = "idx_asset_inv_categories_code", columnList = "category_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_asset_inv_category_task_category_type_code",
                        columnNames = {"task_id", "asset_category", "asset_type", "category_code"}
                )
        }
)
public class AssetInventoryCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @Column(name = "asset_category", nullable = false, length = 255)
    private String assetCategory;

    @Column(name = "asset_type", nullable = false, length = 255)
    private String assetType;

    @Column(name = "category_code", nullable = false, length = 100)
    private String categoryCode;

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

    public String getAssetCategory() { return assetCategory; }
    public void setAssetCategory(String assetCategory) { this.assetCategory = assetCategory; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public Integer getTotalAssets() { return totalAssets; }
    public void setTotalAssets(Integer totalAssets) { this.totalAssets = totalAssets; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
