package com.pinetechs.orvix.ims.inventory.asset.dto;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryCategory;

public class AssetInventoryCategoryResponse {
    private Long id;
    private Long taskId;
    private String assetCategory;
    private String assetType;
    private String categoryCode;
    private Integer totalAssets;

    public static AssetInventoryCategoryResponse from(AssetInventoryCategory category) {
        AssetInventoryCategoryResponse response = new AssetInventoryCategoryResponse();
        response.setId(category.getId());
        response.setTaskId(category.getInventoryTask() == null ? null : category.getInventoryTask().getId());
        response.setAssetCategory(category.getAssetCategory());
        response.setAssetType(category.getAssetType());
        response.setCategoryCode(category.getCategoryCode());
        response.setTotalAssets(category.getTotalAssets());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getAssetCategory() { return assetCategory; }
    public void setAssetCategory(String assetCategory) { this.assetCategory = assetCategory; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public Integer getTotalAssets() { return totalAssets; }
    public void setTotalAssets(Integer totalAssets) { this.totalAssets = totalAssets; }
}
