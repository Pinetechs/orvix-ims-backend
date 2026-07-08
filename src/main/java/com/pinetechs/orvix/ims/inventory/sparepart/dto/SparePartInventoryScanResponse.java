package com.pinetechs.orvix.ims.inventory.sparepart.dto;

import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartInventoryItemStatus;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartInventoryLocationStatus;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartInventoryQuantityStatus;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartInventoryScanResult;

import java.math.BigDecimal;

public class SparePartInventoryScanResponse {
    private Long scanId;
    private Long itemId;
    private String itemNo;
    private SparePartInventoryScanResult scanResult;
    private SparePartInventoryItemStatus itemStatus;
    private SparePartInventoryLocationStatus locationStatus;
    private SparePartInventoryQuantityStatus quantityStatus;
    private String message;
    private Long expectedBranchId;
    private String expectedBranchName;
    private Long expectedLocationId;
    private String expectedLocationCode;
    private Long actualBranchId;
    private String actualBranchName;
    private Long actualLocationId;
    private String actualLocationCode;
    private BigDecimal stockQty;
    private BigDecimal countedQty;
    private BigDecimal varianceQty;

    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getItemNo() { return itemNo; }
    public void setItemNo(String itemNo) { this.itemNo = itemNo; }
    public SparePartInventoryScanResult getScanResult() { return scanResult; }
    public void setScanResult(SparePartInventoryScanResult scanResult) { this.scanResult = scanResult; }
    public SparePartInventoryItemStatus getItemStatus() { return itemStatus; }
    public void setItemStatus(SparePartInventoryItemStatus itemStatus) { this.itemStatus = itemStatus; }
    public SparePartInventoryLocationStatus getLocationStatus() { return locationStatus; }
    public void setLocationStatus(SparePartInventoryLocationStatus locationStatus) { this.locationStatus = locationStatus; }
    public SparePartInventoryQuantityStatus getQuantityStatus() { return quantityStatus; }
    public void setQuantityStatus(SparePartInventoryQuantityStatus quantityStatus) { this.quantityStatus = quantityStatus; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getExpectedBranchId() { return expectedBranchId; }
    public void setExpectedBranchId(Long expectedBranchId) { this.expectedBranchId = expectedBranchId; }
    public String getExpectedBranchName() { return expectedBranchName; }
    public void setExpectedBranchName(String expectedBranchName) { this.expectedBranchName = expectedBranchName; }
    public Long getExpectedLocationId() { return expectedLocationId; }
    public void setExpectedLocationId(Long expectedLocationId) { this.expectedLocationId = expectedLocationId; }
    public String getExpectedLocationCode() { return expectedLocationCode; }
    public void setExpectedLocationCode(String expectedLocationCode) { this.expectedLocationCode = expectedLocationCode; }
    public Long getActualBranchId() { return actualBranchId; }
    public void setActualBranchId(Long actualBranchId) { this.actualBranchId = actualBranchId; }
    public String getActualBranchName() { return actualBranchName; }
    public void setActualBranchName(String actualBranchName) { this.actualBranchName = actualBranchName; }
    public Long getActualLocationId() { return actualLocationId; }
    public void setActualLocationId(Long actualLocationId) { this.actualLocationId = actualLocationId; }
    public String getActualLocationCode() { return actualLocationCode; }
    public void setActualLocationCode(String actualLocationCode) { this.actualLocationCode = actualLocationCode; }
    public BigDecimal getStockQty() { return stockQty; }
    public void setStockQty(BigDecimal stockQty) { this.stockQty = stockQty; }
    public BigDecimal getCountedQty() { return countedQty; }
    public void setCountedQty(BigDecimal countedQty) { this.countedQty = countedQty; }
    public BigDecimal getVarianceQty() { return varianceQty; }
    public void setVarianceQty(BigDecimal varianceQty) { this.varianceQty = varianceQty; }
}
