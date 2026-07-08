package com.pinetechs.orvix.ims.inventory.sparepart.dto;

import java.math.BigDecimal;

public class SparePartInventoryScanRequest {
    private String itemNo;
    private Long branchId;
    private Long locationId;
    private BigDecimal countedQty;
    private String notes;

    public String getItemNo() { return itemNo; }
    public void setItemNo(String itemNo) { this.itemNo = itemNo; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public BigDecimal getCountedQty() { return countedQty; }
    public void setCountedQty(BigDecimal countedQty) { this.countedQty = countedQty; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
