package com.pinetechs.orvix.ims.inventory.sparepart.dto;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryItem;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartInventoryItemStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SparePartInventoryItemResponse {
    private Long id;
    private String itemNo;
    private String brandName;
    private Long plannedBranchId;
    private String plannedBranchName;
    private Long plannedLocationId;
    private String plannedLocationCode;
    private Long actualBranchId;
    private String actualBranchName;
    private Long actualLocationId;
    private String actualLocationCode;
    private BigDecimal qty;
    private BigDecimal stockQty;
    private BigDecimal frozenQty;
    private BigDecimal actualQty;
    private BigDecimal varianceQty;
    private SparePartInventoryItemStatus status;
    private LocalDateTime countedAt;

    public static SparePartInventoryItemResponse from(SparePartInventoryItem item) {
        SparePartInventoryItemResponse response = new SparePartInventoryItemResponse();
        response.setId(item.getId());
        response.setItemNo(item.getItemNo());
        response.setBrandName(item.getBrandName());
        response.setPlannedBranchId(item.getPlannedBranch() == null ? null : item.getPlannedBranch().getId());
        response.setPlannedBranchName(item.getPlannedBranch() == null ? null : item.getPlannedBranch().getBranchName());
        response.setPlannedLocationId(item.getPlannedLocation() == null ? null : item.getPlannedLocation().getId());
        response.setPlannedLocationCode(item.getPlannedLocation() == null ? null : item.getPlannedLocation().getLocationCode());
        response.setActualBranchId(item.getActualBranch() == null ? null : item.getActualBranch().getId());
        response.setActualBranchName(item.getActualBranch() == null ? null : item.getActualBranch().getBranchName());
        response.setActualLocationId(item.getActualLocation() == null ? null : item.getActualLocation().getId());
        response.setActualLocationCode(item.getActualLocation() == null ? null : item.getActualLocation().getLocationCode());
        response.setQty(item.getQty());
        response.setStockQty(item.getStockQty());
        response.setFrozenQty(item.getFrozenQty());
        response.setActualQty(item.getActualQty());
        response.setVarianceQty(item.getVarianceQty());
        response.setStatus(item.getStatus());
        response.setCountedAt(item.getCountedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getItemNo() { return itemNo; }
    public void setItemNo(String itemNo) { this.itemNo = itemNo; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public Long getPlannedBranchId() { return plannedBranchId; }
    public void setPlannedBranchId(Long plannedBranchId) { this.plannedBranchId = plannedBranchId; }
    public String getPlannedBranchName() { return plannedBranchName; }
    public void setPlannedBranchName(String plannedBranchName) { this.plannedBranchName = plannedBranchName; }
    public Long getPlannedLocationId() { return plannedLocationId; }
    public void setPlannedLocationId(Long plannedLocationId) { this.plannedLocationId = plannedLocationId; }
    public String getPlannedLocationCode() { return plannedLocationCode; }
    public void setPlannedLocationCode(String plannedLocationCode) { this.plannedLocationCode = plannedLocationCode; }
    public Long getActualBranchId() { return actualBranchId; }
    public void setActualBranchId(Long actualBranchId) { this.actualBranchId = actualBranchId; }
    public String getActualBranchName() { return actualBranchName; }
    public void setActualBranchName(String actualBranchName) { this.actualBranchName = actualBranchName; }
    public Long getActualLocationId() { return actualLocationId; }
    public void setActualLocationId(Long actualLocationId) { this.actualLocationId = actualLocationId; }
    public String getActualLocationCode() { return actualLocationCode; }
    public void setActualLocationCode(String actualLocationCode) { this.actualLocationCode = actualLocationCode; }
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
    public LocalDateTime getCountedAt() { return countedAt; }
    public void setCountedAt(LocalDateTime countedAt) { this.countedAt = countedAt; }
}
