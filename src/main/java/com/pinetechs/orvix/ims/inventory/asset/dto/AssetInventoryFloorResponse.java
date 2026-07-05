package com.pinetechs.orvix.ims.inventory.asset.dto;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryFloor;

public class AssetInventoryFloorResponse {
    private Long id;
    private Long taskId;
    private Long locationId;
    private String locationName;
    private String floorName;
    private Integer totalAssets;

    public static AssetInventoryFloorResponse from(AssetInventoryFloor floor) {
        AssetInventoryFloorResponse response = new AssetInventoryFloorResponse();
        response.setId(floor.getId());
        response.setTaskId(floor.getInventoryTask() == null ? null : floor.getInventoryTask().getId());
        response.setLocationId(floor.getLocation() == null ? null : floor.getLocation().getId());
        response.setLocationName(floor.getLocation() == null ? null : floor.getLocation().getLocationName());
        response.setFloorName(floor.getFloorName());
        response.setTotalAssets(floor.getTotalAssets());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getFloorName() { return floorName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }
    public Integer getTotalAssets() { return totalAssets; }
    public void setTotalAssets(Integer totalAssets) { this.totalAssets = totalAssets; }
}
