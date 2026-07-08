package com.pinetechs.orvix.ims.inventory.sparepart.entity;

import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartInventoryItemStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "spare_part_inventory_items",
        indexes = {
                @Index(name = "idx_sp_inv_items_task", columnList = "task_id"),
                @Index(name = "idx_sp_inv_items_item_no", columnList = "item_no"),
                @Index(name = "idx_sp_inv_items_status", columnList = "status"),
                @Index(name = "idx_sp_inv_items_planned_branch", columnList = "planned_branch_id"),
                @Index(name = "idx_sp_inv_items_planned_location", columnList = "planned_location_id"),
                @Index(name = "idx_sp_inv_items_actual_branch", columnList = "actual_branch_id"),
                @Index(name = "idx_sp_inv_items_actual_location", columnList = "actual_location_id"),
                @Index(name = "idx_sp_inv_items_brand", columnList = "brand_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sp_inv_item_task_item_branch_location",
                        columnNames = {"task_id", "item_no", "planned_branch_id", "planned_location_id"}
                )
        }
)
public class SparePartInventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @Column(name = "item_no", nullable = false, length = 150)
    private String itemNo;

    @Column(name = "brand_name", length = 255)
    private String brandName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private SparePartInventoryBrand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planned_branch_id", nullable = false)
    private SparePartInventoryBranch plannedBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planned_location_id", nullable = false)
    private SparePartInventoryLocation plannedLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actual_branch_id")
    private SparePartInventoryBranch actualBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actual_location_id")
    private SparePartInventoryLocation actualLocation;

    @Column(name = "qty", precision = 18, scale = 3)
    private BigDecimal qty;

    @Column(name = "stock_qty", precision = 18, scale = 3)
    private BigDecimal stockQty;

    @Column(name = "frozen_qty", precision = 18, scale = 3)
    private BigDecimal frozenQty;

    @Column(name = "actual_qty", precision = 18, scale = 3)
    private BigDecimal actualQty;

    @Column(name = "variance_qty", precision = 18, scale = 3)
    private BigDecimal varianceQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 80)
    private SparePartInventoryItemStatus status = SparePartInventoryItemStatus.NOT_COUNTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counted_by_user_id")
    private User countedBy;

    @Column(name = "counted_at")
    private LocalDateTime countedAt;

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
    public String getItemNo() { return itemNo; }
    public void setItemNo(String itemNo) { this.itemNo = itemNo; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public SparePartInventoryBrand getBrand() { return brand; }
    public void setBrand(SparePartInventoryBrand brand) { this.brand = brand; }
    public SparePartInventoryBranch getPlannedBranch() { return plannedBranch; }
    public void setPlannedBranch(SparePartInventoryBranch plannedBranch) { this.plannedBranch = plannedBranch; }
    public SparePartInventoryLocation getPlannedLocation() { return plannedLocation; }
    public void setPlannedLocation(SparePartInventoryLocation plannedLocation) { this.plannedLocation = plannedLocation; }
    public SparePartInventoryBranch getActualBranch() { return actualBranch; }
    public void setActualBranch(SparePartInventoryBranch actualBranch) { this.actualBranch = actualBranch; }
    public SparePartInventoryLocation getActualLocation() { return actualLocation; }
    public void setActualLocation(SparePartInventoryLocation actualLocation) { this.actualLocation = actualLocation; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
    public BigDecimal getStockQty() { return stockQty; }
    public void setStockQty(BigDecimal stockQty) { this.stockQty = stockQty; }
    public BigDecimal getFrozenQty() { return frozenQty; }
    public void setFrozenQty(BigDecimal frozenQty) { this.frozenQty = frozenQty; }
    public BigDecimal getActualQty() { return actualQty; }
    public void setActualQty(BigDecimal actualQty) { this.actualQty = actualQty; }
    public BigDecimal getVarianceQty() { return varianceQty; }
    public void setVarianceQty(BigDecimal varianceQty) { this.varianceQty = varianceQty; }
    public SparePartInventoryItemStatus getStatus() { return status; }
    public void setStatus(SparePartInventoryItemStatus status) { this.status = status; }
    public User getCountedBy() { return countedBy; }
    public void setCountedBy(User countedBy) { this.countedBy = countedBy; }
    public LocalDateTime getCountedAt() { return countedAt; }
    public void setCountedAt(LocalDateTime countedAt) { this.countedAt = countedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
