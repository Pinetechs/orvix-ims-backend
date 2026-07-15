package com.pinetechs.orvix.ims.inventory.asset.entity;

import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryItemStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "asset_inventory_items",
        indexes = {
                @Index(name = "idx_asset_inv_items_task", columnList = "task_id"),
                @Index(name = "idx_asset_inv_items_barcode", columnList = "barcode"),
                @Index(name = "idx_asset_inv_items_status", columnList = "status"),
                @Index(name = "idx_asset_inv_items_planned_location", columnList = "planned_location_id"),
                @Index(name = "idx_asset_inv_items_planned_floor", columnList = "planned_floor_id"),
                @Index(name = "idx_asset_inv_items_planned_place", columnList = "planned_place_id"),
                @Index(name = "idx_asset_inv_items_actual_location", columnList = "actual_location_id"),
                @Index(name = "idx_asset_inv_items_actual_floor", columnList = "actual_floor_id"),
                @Index(name = "idx_asset_inv_items_actual_place", columnList = "actual_place_id"),
                @Index(name = "idx_asset_inv_items_category", columnList = "category_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_asset_inv_item_task_barcode",
                        columnNames = {"task_id", "barcode"}
                )
        }
)
public class AssetInventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @Column(name = "barcode", nullable = false, length = 100)
    private String barcode;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "asset_category", length = 255)
    private String assetCategory;

    @Column(name = "asset_type", length = 255)
    private String assetType;

    @Column(name = "category_code", length = 100)
    private String categoryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private AssetInventoryCategory category;

    @Column(name = "quantity", precision = 18, scale = 3)
    private BigDecimal quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planned_location_id", nullable = false)
    private AssetInventoryLocation plannedLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planned_floor_id", nullable = false)
    private AssetInventoryFloor plannedFloor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planned_place_id", nullable = false)
    private AssetInventoryPlace plannedPlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actual_location_id")
    private AssetInventoryLocation actualLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actual_floor_id")
    private AssetInventoryFloor actualFloor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actual_place_id")
    private AssetInventoryPlace actualPlace;

    @Column(name = "market_value_weight_per_item", precision = 18, scale = 8)
    private BigDecimal marketValueWeightPerItem;

    @Column(name = "market_value", precision = 18, scale = 3)
    private BigDecimal marketValue;

    @Column(name = "extra_value_per_item", precision = 18, scale = 3)
    private BigDecimal extraValuePerItem;

    @Column(name = "new_acc_dep", precision = 18, scale = 3)
    private BigDecimal newAccDep;

    @Column(name = "purchase_value_market_value", precision = 18, scale = 3)
    private BigDecimal purchaseValueMarketValue;

    @Column(name = "new_old_purchase_value", precision = 18, scale = 3)
    private BigDecimal newOldPurchaseValue;

    @Column(name = "new_purchase_value", precision = 18, scale = 3)
    private BigDecimal newPurchaseValue;

    @Column(name = "old_purchase_value", precision = 18, scale = 3)
    private BigDecimal oldPurchaseValue;

    @Column(name = "final_purchase_value", precision = 18, scale = 3)
    private BigDecimal finalPurchaseValue;

    @Column(name = "final_acc_dep", precision = 18, scale = 3)
    private BigDecimal finalAccDep;

    @Column(name = "final_book_value", precision = 18, scale = 3)
    private BigDecimal finalBookValue;

    @Column(name = "acc_dep", precision = 18, scale = 3)
    private BigDecimal accDep;

    @Column(name = "book_value", precision = 18, scale = 3)
    private BigDecimal bookValue;

    @Column(name = "status_ratio", precision = 10, scale = 4)
    private BigDecimal statusRatio;

    @Column(name = "asset_condition", length = 255)
    private String assetCondition;

    @Column(name = "branch_code", length = 100)
    private String branchCode;

    @Column(name = "main_dep_code", length = 100)
    private String mainDepCode;

    @Column(name = "asset_date")
    private LocalDate assetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AssetInventoryItemStatus status = AssetInventoryItemStatus.NOT_SCANNED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by_user_id")
    private User checkedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_scan_id")
    private AssetInventoryScan currentScan;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

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
    public InventoryTask getInventoryTask() { return inventoryTask; }
    public void setInventoryTask(InventoryTask inventoryTask) { this.inventoryTask = inventoryTask; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssetCategory() { return assetCategory; }
    public void setAssetCategory(String assetCategory) { this.assetCategory = assetCategory; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public AssetInventoryCategory getCategory() { return category; }
    public void setCategory(AssetInventoryCategory category) { this.category = category; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public AssetInventoryLocation getPlannedLocation() { return plannedLocation; }
    public void setPlannedLocation(AssetInventoryLocation plannedLocation) { this.plannedLocation = plannedLocation; }
    public AssetInventoryFloor getPlannedFloor() { return plannedFloor; }
    public void setPlannedFloor(AssetInventoryFloor plannedFloor) { this.plannedFloor = plannedFloor; }
    public AssetInventoryPlace getPlannedPlace() { return plannedPlace; }
    public void setPlannedPlace(AssetInventoryPlace plannedPlace) { this.plannedPlace = plannedPlace; }
    public AssetInventoryLocation getActualLocation() { return actualLocation; }
    public void setActualLocation(AssetInventoryLocation actualLocation) { this.actualLocation = actualLocation; }
    public AssetInventoryFloor getActualFloor() { return actualFloor; }
    public void setActualFloor(AssetInventoryFloor actualFloor) { this.actualFloor = actualFloor; }
    public AssetInventoryPlace getActualPlace() { return actualPlace; }
    public void setActualPlace(AssetInventoryPlace actualPlace) { this.actualPlace = actualPlace; }
    public BigDecimal getMarketValueWeightPerItem() { return marketValueWeightPerItem; }
    public void setMarketValueWeightPerItem(BigDecimal marketValueWeightPerItem) { this.marketValueWeightPerItem = marketValueWeightPerItem; }
    public BigDecimal getMarketValue() { return marketValue; }
    public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }
    public BigDecimal getExtraValuePerItem() { return extraValuePerItem; }
    public void setExtraValuePerItem(BigDecimal extraValuePerItem) { this.extraValuePerItem = extraValuePerItem; }
    public BigDecimal getNewAccDep() { return newAccDep; }
    public void setNewAccDep(BigDecimal newAccDep) { this.newAccDep = newAccDep; }
    public BigDecimal getPurchaseValueMarketValue() { return purchaseValueMarketValue; }
    public void setPurchaseValueMarketValue(BigDecimal purchaseValueMarketValue) { this.purchaseValueMarketValue = purchaseValueMarketValue; }
    public BigDecimal getNewOldPurchaseValue() { return newOldPurchaseValue; }
    public void setNewOldPurchaseValue(BigDecimal newOldPurchaseValue) { this.newOldPurchaseValue = newOldPurchaseValue; }
    public BigDecimal getNewPurchaseValue() { return newPurchaseValue; }
    public void setNewPurchaseValue(BigDecimal newPurchaseValue) { this.newPurchaseValue = newPurchaseValue; }
    public BigDecimal getOldPurchaseValue() { return oldPurchaseValue; }
    public void setOldPurchaseValue(BigDecimal oldPurchaseValue) { this.oldPurchaseValue = oldPurchaseValue; }
    public BigDecimal getFinalPurchaseValue() { return finalPurchaseValue; }
    public void setFinalPurchaseValue(BigDecimal finalPurchaseValue) { this.finalPurchaseValue = finalPurchaseValue; }
    public BigDecimal getFinalAccDep() { return finalAccDep; }
    public void setFinalAccDep(BigDecimal finalAccDep) { this.finalAccDep = finalAccDep; }
    public BigDecimal getFinalBookValue() { return finalBookValue; }
    public void setFinalBookValue(BigDecimal finalBookValue) { this.finalBookValue = finalBookValue; }
    public BigDecimal getAccDep() { return accDep; }
    public void setAccDep(BigDecimal accDep) { this.accDep = accDep; }
    public BigDecimal getBookValue() { return bookValue; }
    public void setBookValue(BigDecimal bookValue) { this.bookValue = bookValue; }
    public BigDecimal getStatusRatio() { return statusRatio; }
    public void setStatusRatio(BigDecimal statusRatio) { this.statusRatio = statusRatio; }
    public String getAssetCondition() { return assetCondition; }
    public void setAssetCondition(String assetCondition) { this.assetCondition = assetCondition; }
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    public String getMainDepCode() { return mainDepCode; }
    public void setMainDepCode(String mainDepCode) { this.mainDepCode = mainDepCode; }
    public LocalDate getAssetDate() { return assetDate; }
    public void setAssetDate(LocalDate assetDate) { this.assetDate = assetDate; }
    public AssetInventoryItemStatus getStatus() { return status; }
    public void setStatus(AssetInventoryItemStatus status) { this.status = status; }
    public User getCheckedBy() { return checkedBy; }
    public void setCheckedBy(User checkedBy) { this.checkedBy = checkedBy; }
    public AssetInventoryScan getCurrentScan() { return currentScan; }
    public void setCurrentScan(AssetInventoryScan currentScan) { this.currentScan = currentScan; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
