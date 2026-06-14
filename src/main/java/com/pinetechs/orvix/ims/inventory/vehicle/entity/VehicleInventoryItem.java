package com.pinetechs.orvix.ims.inventory.vehicle.entity;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.vehicle.enums.VehicleInventoryItemStatus;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "vehicle_inventory_items",
        indexes = {
                @Index(name = "idx_vehicle_inventory_items_task", columnList = "task_id"),
                @Index(name = "idx_vehicle_inventory_items_vin_no", columnList = "vin_no"),
                @Index(name = "idx_vehicle_inventory_items_store_no", columnList = "store_no"),
                @Index(name = "idx_vehicle_inventory_items_location", columnList = "location"),
                @Index(name = "idx_vehicle_inventory_items_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vehicle_inventory_item_task_vin",
                        columnNames = {"task_id", "vin_no"}
                )
        }
)
public class VehicleInventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Parent inventory task
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    // ERP fields
    @Column(name = "part_no", length = 100)
    private String partNo;

    @Column(name = "make", length = 100)
    private String make;

    @Column(name = "model_name", length = 150)
    private String modelName;

    @Column(name = "model_year")
    private Integer modelYear;

    @Column(name = "vin_no", nullable = false, length = 100)
    private String vinNo;

    @Column(name = "specification", length = 500)
    private String specification;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Column(name = "color_no", length = 100)
    private String colorNo;

    @Column(name = "interior_color", length = 100)
    private String interiorColor;

    @Column(name = "mch_status", length = 100)
    private String mchStatus;

    @Column(name = "stock_status", length = 100)
    private String stockStatus;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "store_no", length = 100)
    private String storeNo;

    @Column(name = "dar_art_id", length = 100)
    private String darArtId;

    // Inventory process fields
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private VehicleInventoryItemStatus status = VehicleInventoryItemStatus.PENDING;

    @Column(name = "actual_location", length = 255)
    private String actualLocation;

    @Column(name = "actual_store_no", length = 100)
    private String actualStoreNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by_user_id")
    private User checkedBy;

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

    public String getPartNo() { return partNo; }
    public void setPartNo(String partNo) { this.partNo = partNo; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }

    public String getVinNo() { return vinNo; }
    public void setVinNo(String vinNo) { this.vinNo = vinNo; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public LocalDate getReceiptDate() { return receiptDate; }
    public void setReceiptDate(LocalDate receiptDate) { this.receiptDate = receiptDate; }

    public String getColorNo() { return colorNo; }
    public void setColorNo(String colorNo) { this.colorNo = colorNo; }

    public String getInteriorColor() { return interiorColor; }
    public void setInteriorColor(String interiorColor) { this.interiorColor = interiorColor; }

    public String getMchStatus() { return mchStatus; }
    public void setMchStatus(String mchStatus) { this.mchStatus = mchStatus; }

    public String getStockStatus() { return stockStatus; }
    public void setStockStatus(String stockStatus) { this.stockStatus = stockStatus; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStoreNo() { return storeNo; }
    public void setStoreNo(String storeNo) { this.storeNo = storeNo; }

    public String getDarArtId() { return darArtId; }
    public void setDarArtId(String darArtId) { this.darArtId = darArtId; }

    public VehicleInventoryItemStatus getStatus() { return status; }
    public void setStatus(VehicleInventoryItemStatus status) { this.status = status; }

    public String getActualLocation() { return actualLocation; }
    public void setActualLocation(String actualLocation) { this.actualLocation = actualLocation; }

    public String getActualStoreNo() { return actualStoreNo; }
    public void setActualStoreNo(String actualStoreNo) { this.actualStoreNo = actualStoreNo; }

    public User getCheckedBy() { return checkedBy; }
    public void setCheckedBy(User checkedBy) { this.checkedBy = checkedBy; }

    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}