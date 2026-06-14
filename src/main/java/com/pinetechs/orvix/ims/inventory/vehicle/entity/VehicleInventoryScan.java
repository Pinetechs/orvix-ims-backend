package com.pinetechs.orvix.ims.inventory.vehicle.entity;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.vehicle.enums.VehicleInventoryScanResult;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "vehicle_inventory_scans",
        indexes = {
                @Index(name = "idx_vehicle_inventory_scans_task", columnList = "task_id"),
                @Index(name = "idx_vehicle_inventory_scans_item", columnList = "item_id"),
                @Index(name = "idx_vehicle_inventory_scans_user", columnList = "user_id"),
                @Index(name = "idx_vehicle_inventory_scans_vin", columnList = "scanned_vin"),
                @Index(name = "idx_vehicle_inventory_scans_result", columnList = "scan_result")
        }
)
public class VehicleInventoryScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Parent task for fast reporting
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    // Nullable لأن VIN قد يكون غير موجود في الشيت
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private VehicleInventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User scannedBy;

    @Column(name = "scanned_vin", nullable = false, length = 100)
    private String scannedVin;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_result", nullable = false, length = 50)
    private VehicleInventoryScanResult scanResult;

    @Column(name = "expected_store_no", length = 100)
    private String expectedStoreNo;

    @Column(name = "actual_store_no", length = 100)
    private String actualStoreNo;

    @Column(name = "expected_location", length = 255)
    private String expectedLocation;

    @Column(name = "actual_location", length = 255)
    private String actualLocation;

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "scanned_at", nullable = false, updatable = false)
    private LocalDateTime scannedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InventoryTask getInventoryTask() { return inventoryTask; }
    public void setInventoryTask(InventoryTask inventoryTask) { this.inventoryTask = inventoryTask; }

    public VehicleInventoryItem getItem() { return item; }
    public void setItem(VehicleInventoryItem item) { this.item = item; }

    public User getScannedBy() { return scannedBy; }
    public void setScannedBy(User scannedBy) { this.scannedBy = scannedBy; }

    public String getScannedVin() { return scannedVin; }
    public void setScannedVin(String scannedVin) { this.scannedVin = scannedVin; }

    public VehicleInventoryScanResult getScanResult() { return scanResult; }
    public void setScanResult(VehicleInventoryScanResult scanResult) { this.scanResult = scanResult; }

    public String getExpectedStoreNo() { return expectedStoreNo; }
    public void setExpectedStoreNo(String expectedStoreNo) { this.expectedStoreNo = expectedStoreNo; }

    public String getActualStoreNo() { return actualStoreNo; }
    public void setActualStoreNo(String actualStoreNo) { this.actualStoreNo = actualStoreNo; }

    public String getExpectedLocation() { return expectedLocation; }
    public void setExpectedLocation(String expectedLocation) { this.expectedLocation = expectedLocation; }

    public String getActualLocation() { return actualLocation; }
    public void setActualLocation(String actualLocation) { this.actualLocation = actualLocation; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getScannedAt() { return scannedAt; }
}