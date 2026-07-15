package com.pinetechs.orvix.ims.inventory.asset.entity;

import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryScanResult;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanImageSource;
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
                @Index(name = "idx_asset_inv_scans_actual_place", columnList = "actual_place_id"),
                @Index(name = "idx_asset_inv_scans_client", columnList = "client_scan_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_asset_scan_task_client",
                columnNames = {"task_id", "client_scan_id"}
        )
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

    @Column(name = "client_scan_id", nullable = false, length = 36)
    private String clientScanId;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private InventoryScanEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrects_scan_id")
    private AssetInventoryScan correctsScan;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_image_file_id", unique = true)
    private UploadedFile scanImage;

    @Column(name = "mismatch_fields", length = 100)
    private String mismatchFields;

    @Column(name = "device_scanned_at")
    private LocalDateTime deviceScannedAt;

    @Column(name = "device_id", length = 150)
    private String deviceId;

    @Column(name = "symbology", length = 80)
    private String symbology;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_source", length = 40)
    private InventoryScanImageSource imageSource;

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
    public String getClientScanId() { return clientScanId; }
    public void setClientScanId(String clientScanId) { this.clientScanId = clientScanId; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public InventoryScanEventType getEventType() { return eventType; }
    public void setEventType(InventoryScanEventType eventType) { this.eventType = eventType; }
    public AssetInventoryScan getCorrectsScan() { return correctsScan; }
    public void setCorrectsScan(AssetInventoryScan correctsScan) { this.correctsScan = correctsScan; }
    public UploadedFile getScanImage() { return scanImage; }
    public void setScanImage(UploadedFile scanImage) { this.scanImage = scanImage; }
    public String getMismatchFields() { return mismatchFields; }
    public void setMismatchFields(String mismatchFields) { this.mismatchFields = mismatchFields; }
    public LocalDateTime getDeviceScannedAt() { return deviceScannedAt; }
    public void setDeviceScannedAt(LocalDateTime deviceScannedAt) { this.deviceScannedAt = deviceScannedAt; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getSymbology() { return symbology; }
    public void setSymbology(String symbology) { this.symbology = symbology; }
    public InventoryScanImageSource getImageSource() { return imageSource; }
    public void setImageSource(InventoryScanImageSource imageSource) { this.imageSource = imageSource; }
}
