package com.pinetechs.orvix.ims.inventory.vehicle.dto;

import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocation;

public class VehicleInventoryLocationResponse {

    private Long id;
    private Long taskId;
    private String storeNo;
    private String locationName;
    private Integer totalVehicles;
    private Integer processedVehicles;
    private Integer matchedVehicles;
    private Integer missingVehicles;
    private double progressPercentage;

    public static VehicleInventoryLocationResponse from(VehicleInventoryLocation location) {
        VehicleInventoryLocationResponse response = new VehicleInventoryLocationResponse();
        response.setId(location.getId());
        response.setStoreNo(location.getStoreNo());
        response.setLocationName(location.getLocationName());
        response.setTotalVehicles(location.getTotalVehicles());
        response.setProcessedVehicles(location.getProcessedVehicles());
        response.setMatchedVehicles(location.getMatchedVehicles());
        response.setMissingVehicles(location.getMissingVehicles());
        response.setProgressPercentage(location.getProgressPercentage());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getStoreNo() {
        return storeNo;
    }

    public void setStoreNo(String storeNo) {
        this.storeNo = storeNo;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Integer getTotalVehicles() {
        return totalVehicles;
    }

    public void setTotalVehicles(Integer totalVehicles) {
        this.totalVehicles = totalVehicles;
    }

    public Integer getProcessedVehicles() {
        return processedVehicles;
    }

    public void setProcessedVehicles(Integer processedVehicles) {
        this.processedVehicles = processedVehicles;
    }

    public Integer getMatchedVehicles() {
        return matchedVehicles;
    }

    public void setMatchedVehicles(Integer matchedVehicles) {
        this.matchedVehicles = matchedVehicles;
    }

    public Integer getMissingVehicles() {
        return missingVehicles;
    }

    public void setMissingVehicles(Integer missingVehicles) {
        this.missingVehicles = missingVehicles;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
}
