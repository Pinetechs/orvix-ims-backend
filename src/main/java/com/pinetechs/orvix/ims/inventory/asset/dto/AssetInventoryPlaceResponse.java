package com.pinetechs.orvix.ims.inventory.asset.dto;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryPlace;

public class AssetInventoryPlaceResponse {
    private Long id;
    private Long taskId;
    private Long locationId;
    private String locationName;
    private Long floorId;
    private String floorName;
    private String placeName;
    private Integer totalAssets;

    public static AssetInventoryPlaceResponse from(AssetInventoryPlace place) {
        AssetInventoryPlaceResponse response = new AssetInventoryPlaceResponse();
        response.setId(place.getId());
        response.setTaskId(place.getInventoryTask() == null ? null : place.getInventoryTask().getId());
        response.setLocationId(place.getLocation() == null ? null : place.getLocation().getId());
        response.setLocationName(place.getLocation() == null ? null : place.getLocation().getLocationName());
        response.setFloorId(place.getFloor() == null ? null : place.getFloor().getId());
        response.setFloorName(place.getFloor() == null ? null : place.getFloor().getFloorName());
        response.setPlaceName(place.getPlaceName());
        response.setTotalAssets(place.getTotalAssets());
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
    public Long getFloorId() { return floorId; }
    public void setFloorId(Long floorId) { this.floorId = floorId; }
    public String getFloorName() { return floorName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }
    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }
    public Integer getTotalAssets() { return totalAssets; }
    public void setTotalAssets(Integer totalAssets) { this.totalAssets = totalAssets; }
}
