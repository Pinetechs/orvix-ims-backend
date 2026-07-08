package com.pinetechs.orvix.ims.inventory.sparepart.entity;

import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartInventoryLocationStatus;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartInventoryQuantityStatus;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartInventoryScanResult;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "spare_part_inventory_scans",
        indexes = {
                @Index(name = "idx_sp_inv_scans_task", columnList = "task_id"),
                @Index(name = "idx_sp_inv_scans_item", columnList = "item_id"),
                @Index(name = "idx_sp_inv_scans_item_no", columnList = "scanned_item_no"),
                @Index(name = "idx_sp_inv_scans_actual_branch", columnList = "actual_branch_id"),
                @Index(name = "idx_sp_inv_scans_actual_location", columnList = "actual_location_id"),
                @Index(name = "idx_sp_inv_scans_result", columnList = "scan_result")
        }
)
public class SparePartInventoryScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private SparePartInventoryItem item;

    @Column(name = "scanned_item_no", nullable = false, length = 150)
    private String scannedItemNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanned_by_user_id", nullable = false)
    private User scannedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expected_branch_id")
    private SparePartInventoryBranch expectedBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expected_location_id")
    private SparePartInventoryLocation expectedLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actual_branch_id", nullable = false)
    private SparePartInventoryBranch actualBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actual_location_id", nullable = false)
    private SparePartInventoryLocation actualLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_status", nullable = false, length = 80)
    private SparePartInventoryLocationStatus locationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_status", nullable = false, length = 80)
    private SparePartInventoryQuantityStatus quantityStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_result", nullable = false, length = 100)
    private SparePartInventoryScanResult scanResult;

    @Column(name = "stock_qty", precision = 18, scale = 3)
    private BigDecimal stockQty;

    @Column(name = "counted_qty", precision = 18, scale = 3)
    private BigDecimal countedQty;

    @Column(name = "variance_qty", precision = 18, scale = 3)
    private BigDecimal varianceQty;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "scanned_at", nullable = false, updatable = false)
    private LocalDateTime scannedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InventoryTask getInventoryTask() { return inventoryTask; }
    public void setInventoryTask(InventoryTask inventoryTask) { this.inventoryTask = inventoryTask; }
    public SparePartInventoryItem getItem() { return item; }
    public void setItem(SparePartInventoryItem item) { this.item = item; }
    public String getScannedItemNo() { return scannedItemNo; }
    public void setScannedItemNo(String scannedItemNo) { this.scannedItemNo = scannedItemNo; }
    public User getScannedBy() { return scannedBy; }
    public void setScannedBy(User scannedBy) { this.scannedBy = scannedBy; }
    public SparePartInventoryBranch getExpectedBranch() { return expectedBranch; }
    public void setExpectedBranch(SparePartInventoryBranch expectedBranch) { this.expectedBranch = expectedBranch; }
    public SparePartInventoryLocation getExpectedLocation() { return expectedLocation; }
    public void setExpectedLocation(SparePartInventoryLocation expectedLocation) { this.expectedLocation = expectedLocation; }
    public SparePartInventoryBranch getActualBranch() { return actualBranch; }
    public void setActualBranch(SparePartInventoryBranch actualBranch) { this.actualBranch = actualBranch; }
    public SparePartInventoryLocation getActualLocation() { return actualLocation; }
    public void setActualLocation(SparePartInventoryLocation actualLocation) { this.actualLocation = actualLocation; }
    public SparePartInventoryLocationStatus getLocationStatus() { return locationStatus; }
    public void setLocationStatus(SparePartInventoryLocationStatus locationStatus) { this.locationStatus = locationStatus; }
    public SparePartInventoryQuantityStatus getQuantityStatus() { return quantityStatus; }
    public void setQuantityStatus(SparePartInventoryQuantityStatus quantityStatus) { this.quantityStatus = quantityStatus; }
    public SparePartInventoryScanResult getScanResult() { return scanResult; }
    public void setScanResult(SparePartInventoryScanResult scanResult) { this.scanResult = scanResult; }
    public BigDecimal getStockQty() { return stockQty; }
    public void setStockQty(BigDecimal stockQty) { this.stockQty = stockQty; }
    public BigDecimal getCountedQty() { return countedQty; }
    public void setCountedQty(BigDecimal countedQty) { this.countedQty = countedQty; }
    public BigDecimal getVarianceQty() { return varianceQty; }
    public void setVarianceQty(BigDecimal varianceQty) { this.varianceQty = varianceQty; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getScannedAt() { return scannedAt; }
}
