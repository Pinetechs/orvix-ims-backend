package com.pinetechs.orvix.ims.inventory.asset.entity;

import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryScanResult;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "asset_inventory_scans",
        indexes = {
                @Index(name = "idx_asset_inv_scans_task", columnList = "task_id"),
                @Index(name = "idx_asset_inv_scans_item", columnList = "item_id"),
                @Index(name = "idx_asset_inv_scans_user", columnList = "user_id"),
                @Index(name = "idx_asset_inv_scans_barcode", columnList = "scanned_barcode"),
                @Index(name = "idx_asset_inv_scans_result", columnList = "scan_result"),
                @Index(name = "idx_asset_inv_scans_actual_location", columnList = "actual_location_id"),
                @Index(name = "idx_asset_inv_scans_actual_floor", columnList = "actual_floor_id"),
                @Index(name = "idx_asset_inv_scans_actual_place", columnList = "actual_place_id")
        }
)
public class AssetInventoryScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private AssetInventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User scannedBy;

    @Column(name = "scanned_barcode", nullable = false, length = 100)
    private String scannedBarcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_result", nullable = false, length = 50)
    private AssetInventoryScanResult scanResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expected_location_id")
    private AssetInventoryLocation expectedLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expected_floor_id")
    private AssetInventoryFloor expectedFloor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expected_place_id")
    private AssetInventoryPlace expectedPlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actual_location_id")
    private AssetInventoryLocation actualLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actual_floor_id")
    private AssetInventoryFloor actualFloor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actual_place_id")
    private AssetInventoryPlace actualPlace;

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "scanned_at", nullable = false, updatable = false)
    private LocalDateTime scannedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InventoryTask getInventoryTask() { return inventoryTask; }
    public void setInventoryTask(InventoryTask inventoryTask) { this.inventoryTask = inventoryTask; }
    public AssetInventoryItem getItem() { return item; }
    public void setItem(AssetInventoryItem item) { this.item = item; }
    public User getScannedBy() { return scannedBy; }
    public void setScannedBy(User scannedBy) { this.scannedBy = scannedBy; }
    public String getScannedBarcode() { return scannedBarcode; }
    public void setScannedBarcode(String scannedBarcode) { this.scannedBarcode = scannedBarcode; }
    public AssetInventoryScanResult getScanResult() { return scanResult; }
    public void setScanResult(AssetInventoryScanResult scanResult) { this.scanResult = scanResult; }
    public AssetInventoryLocation getExpectedLocation() { return expectedLocation; }
    public void setExpectedLocation(AssetInventoryLocation expectedLocation) { this.expectedLocation = expectedLocation; }
    public AssetInventoryFloor getExpectedFloor() { return expectedFloor; }
    public void setExpectedFloor(AssetInventoryFloor expectedFloor) { this.expectedFloor = expectedFloor; }
    public AssetInventoryPlace getExpectedPlace() { return expectedPlace; }
    public void setExpectedPlace(AssetInventoryPlace expectedPlace) { this.expectedPlace = expectedPlace; }
    public AssetInventoryLocation getActualLocation() { return actualLocation; }
    public void setActualLocation(AssetInventoryLocation actualLocation) { this.actualLocation = actualLocation; }
    public AssetInventoryFloor getActualFloor() { return actualFloor; }
    public void setActualFloor(AssetInventoryFloor actualFloor) { this.actualFloor = actualFloor; }
    public AssetInventoryPlace getActualPlace() { return actualPlace; }
    public void setActualPlace(AssetInventoryPlace actualPlace) { this.actualPlace = actualPlace; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getScannedAt() { return scannedAt; }
}
