package com.pinetechs.orvix.ims.inventory.asset.dto;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryLocation;

public class AssetInventoryLocationResponse {

    private Long id;
    private Long taskId;
    private String locationName;
    private Integer totalAssets;
    private Integer processedAssets;
    private Integer matchedAssets;
    private Integer missingAssets;
    private double progressPercentage;

    public static AssetInventoryLocationResponse from(AssetInventoryLocation location) {
        AssetInventoryLocationResponse response = new AssetInventoryLocationResponse();
        response.setId(location.getId());
        response.setTaskId(location.getInventoryTask() == null ? null : location.getInventoryTask().getId());
        response.setLocationName(location.getLocationName());
        response.setTotalAssets(location.getTotalAssets());
        response.setProcessedAssets(location.getProcessedAssets());
        response.setMatchedAssets(location.getMatchedAssets());
        response.setMissingAssets(location.getMissingAssets());
        response.setProgressPercentage(location.getProgressPercentage());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public Integer getTotalAssets() { return totalAssets; }
    public void setTotalAssets(Integer totalAssets) { this.totalAssets = totalAssets; }
    public Integer getProcessedAssets() { return processedAssets; }
    public void setProcessedAssets(Integer processedAssets) { this.processedAssets = processedAssets; }
    public Integer getMatchedAssets() { return matchedAssets; }
    public void setMatchedAssets(Integer matchedAssets) { this.matchedAssets = matchedAssets; }
    public Integer getMissingAssets() { return missingAssets; }
    public void setMissingAssets(Integer missingAssets) { this.missingAssets = missingAssets; }
    public double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(double progressPercentage) { this.progressPercentage = progressPercentage; }
}
