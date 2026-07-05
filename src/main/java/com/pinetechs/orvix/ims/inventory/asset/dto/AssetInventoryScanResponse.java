package com.pinetechs.orvix.ims.inventory.asset.dto;

import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryItemStatus;
import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryScanResult;

public class AssetInventoryScanResponse {
    private Long scanId;
    private Long itemId;
    private String barcode;
    private AssetInventoryScanResult scanResult;
    private AssetInventoryItemStatus itemStatus;
    private String message;
    private Long expectedLocationId;
    private String expectedLocationName;
    private Long expectedFloorId;
    private String expectedFloorName;
    private Long expectedPlaceId;
    private String expectedPlaceName;
    private Long actualLocationId;
    private String actualLocationName;
    private Long actualFloorId;
    private String actualFloorName;
    private Long actualPlaceId;
    private String actualPlaceName;

    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public AssetInventoryScanResult getScanResult() { return scanResult; }
    public void setScanResult(AssetInventoryScanResult scanResult) { this.scanResult = scanResult; }
    public AssetInventoryItemStatus getItemStatus() { return itemStatus; }
    public void setItemStatus(AssetInventoryItemStatus itemStatus) { this.itemStatus = itemStatus; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getExpectedLocationId() { return expectedLocationId; }
    public void setExpectedLocationId(Long expectedLocationId) { this.expectedLocationId = expectedLocationId; }
    public String getExpectedLocationName() { return expectedLocationName; }
    public void setExpectedLocationName(String expectedLocationName) { this.expectedLocationName = expectedLocationName; }
    public Long getExpectedFloorId() { return expectedFloorId; }
    public void setExpectedFloorId(Long expectedFloorId) { this.expectedFloorId = expectedFloorId; }
    public String getExpectedFloorName() { return expectedFloorName; }
    public void setExpectedFloorName(String expectedFloorName) { this.expectedFloorName = expectedFloorName; }
    public Long getExpectedPlaceId() { return expectedPlaceId; }
    public void setExpectedPlaceId(Long expectedPlaceId) { this.expectedPlaceId = expectedPlaceId; }
    public String getExpectedPlaceName() { return expectedPlaceName; }
    public void setExpectedPlaceName(String expectedPlaceName) { this.expectedPlaceName = expectedPlaceName; }
    public Long getActualLocationId() { return actualLocationId; }
    public void setActualLocationId(Long actualLocationId) { this.actualLocationId = actualLocationId; }
    public String getActualLocationName() { return actualLocationName; }
    public void setActualLocationName(String actualLocationName) { this.actualLocationName = actualLocationName; }
    public Long getActualFloorId() { return actualFloorId; }
    public void setActualFloorId(Long actualFloorId) { this.actualFloorId = actualFloorId; }
    public String getActualFloorName() { return actualFloorName; }
    public void setActualFloorName(String actualFloorName) { this.actualFloorName = actualFloorName; }
    public Long getActualPlaceId() { return actualPlaceId; }
    public void setActualPlaceId(Long actualPlaceId) { this.actualPlaceId = actualPlaceId; }
    public String getActualPlaceName() { return actualPlaceName; }
    public void setActualPlaceName(String actualPlaceName) { this.actualPlaceName = actualPlaceName; }
}
