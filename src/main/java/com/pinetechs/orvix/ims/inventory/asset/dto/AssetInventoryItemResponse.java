package com.pinetechs.orvix.ims.inventory.asset.dto;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryItem;
import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryItemStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AssetInventoryItemResponse {
    private Long id;
    private String barcode;
    private String description;
    private String assetCategory;
    private String assetType;
    private String categoryCode;
    private BigDecimal quantity;
    private Long plannedLocationId;
    private String plannedLocationName;
    private Long plannedFloorId;
    private String plannedFloorName;
    private Long plannedPlaceId;
    private String plannedPlaceName;
    private Long actualLocationId;
    private String actualLocationName;
    private Long actualFloorId;
    private String actualFloorName;
    private Long actualPlaceId;
    private String actualPlaceName;
    private BigDecimal bookValue;
    private BigDecimal finalBookValue;
    private String assetCondition;
    private String branchCode;
    private String mainDepCode;
    private LocalDate assetDate;
    private AssetInventoryItemStatus status;
    private LocalDateTime checkedAt;

    public static AssetInventoryItemResponse from(AssetInventoryItem item) {
        AssetInventoryItemResponse response = new AssetInventoryItemResponse();
        response.setId(item.getId());
        response.setBarcode(item.getBarcode());
        response.setDescription(item.getDescription());
        response.setAssetCategory(item.getAssetCategory());
        response.setAssetType(item.getAssetType());
        response.setCategoryCode(item.getCategoryCode());
        response.setQuantity(item.getQuantity());
        response.setPlannedLocationId(item.getPlannedLocation() == null ? null : item.getPlannedLocation().getId());
        response.setPlannedLocationName(item.getPlannedLocation() == null ? null : item.getPlannedLocation().getLocationName());
        response.setPlannedFloorId(item.getPlannedFloor() == null ? null : item.getPlannedFloor().getId());
        response.setPlannedFloorName(item.getPlannedFloor() == null ? null : item.getPlannedFloor().getFloorName());
        response.setPlannedPlaceId(item.getPlannedPlace() == null ? null : item.getPlannedPlace().getId());
        response.setPlannedPlaceName(item.getPlannedPlace() == null ? null : item.getPlannedPlace().getPlaceName());
        response.setActualLocationId(item.getActualLocation() == null ? null : item.getActualLocation().getId());
        response.setActualLocationName(item.getActualLocation() == null ? null : item.getActualLocation().getLocationName());
        response.setActualFloorId(item.getActualFloor() == null ? null : item.getActualFloor().getId());
        response.setActualFloorName(item.getActualFloor() == null ? null : item.getActualFloor().getFloorName());
        response.setActualPlaceId(item.getActualPlace() == null ? null : item.getActualPlace().getId());
        response.setActualPlaceName(item.getActualPlace() == null ? null : item.getActualPlace().getPlaceName());
        response.setBookValue(item.getBookValue());
        response.setFinalBookValue(item.getFinalBookValue());
        response.setAssetCondition(item.getAssetCondition());
        response.setBranchCode(item.getBranchCode());
        response.setMainDepCode(item.getMainDepCode());
        response.setAssetDate(item.getAssetDate());
        response.setStatus(item.getStatus());
        response.setCheckedAt(item.getCheckedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssetCategory() { return assetCategory; }
    public void setAssetCategory(String assetCategory) { this.assetCategory = assetCategory; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public Long getPlannedLocationId() { return plannedLocationId; }
    public void setPlannedLocationId(Long plannedLocationId) { this.plannedLocationId = plannedLocationId; }
    public String getPlannedLocationName() { return plannedLocationName; }
    public void setPlannedLocationName(String plannedLocationName) { this.plannedLocationName = plannedLocationName; }
    public Long getPlannedFloorId() { return plannedFloorId; }
    public void setPlannedFloorId(Long plannedFloorId) { this.plannedFloorId = plannedFloorId; }
    public String getPlannedFloorName() { return plannedFloorName; }
    public void setPlannedFloorName(String plannedFloorName) { this.plannedFloorName = plannedFloorName; }
    public Long getPlannedPlaceId() { return plannedPlaceId; }
    public void setPlannedPlaceId(Long plannedPlaceId) { this.plannedPlaceId = plannedPlaceId; }
    public String getPlannedPlaceName() { return plannedPlaceName; }
    public void setPlannedPlaceName(String plannedPlaceName) { this.plannedPlaceName = plannedPlaceName; }
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
    public BigDecimal getBookValue() { return bookValue; }
    public void setBookValue(BigDecimal bookValue) { this.bookValue = bookValue; }
    public BigDecimal getFinalBookValue() { return finalBookValue; }
    public void setFinalBookValue(BigDecimal finalBookValue) { this.finalBookValue = finalBookValue; }
    public String getAssetCondition() { return assetCondition; }
    public void setAssetCondition(String assetCondition) { this.assetCondition = assetCondition; }
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    public String getMainDepCode() { return mainDepCode; }
    public void setMainDepCode(String mainDepCode) { this.mainDepCode = mainDepCode; }
    public LocalDate getAssetDate() { return assetDate; }
    public void setAssetDate(LocalDate assetDate) { this.assetDate = assetDate; }
    public AssetInventoryItemStatus getStatus() { return status; }
    public void setStatus(AssetInventoryItemStatus status) { this.status = status; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
}
