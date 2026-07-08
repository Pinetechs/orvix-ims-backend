package com.pinetechs.orvix.ims.inventory.sparepart.dto;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryLocation;

public class SparePartInventoryLocationResponse {
    private Long id;
    private Long taskId;
    private Long branchId;
    private String branchName;
    private String locationCode;
    private Integer totalItems;
    private Integer countedItems;
    private Integer matchedItems;
    private Integer shortageItems;
    private Integer overageItems;
    private Integer locationMismatchItems;
    private double progressPercentage;

    public static SparePartInventoryLocationResponse from(SparePartInventoryLocation location) {
        SparePartInventoryLocationResponse response = new SparePartInventoryLocationResponse();
        response.setId(location.getId());
        response.setTaskId(location.getInventoryTask() == null ? null : location.getInventoryTask().getId());
        response.setBranchId(location.getBranch() == null ? null : location.getBranch().getId());
        response.setBranchName(location.getBranch() == null ? null : location.getBranch().getBranchName());
        response.setLocationCode(location.getLocationCode());
        response.setTotalItems(location.getTotalItems());
        response.setCountedItems(location.getCountedItems());
        response.setMatchedItems(location.getMatchedItems());
        response.setShortageItems(location.getShortageItems());
        response.setOverageItems(location.getOverageItems());
        response.setLocationMismatchItems(location.getLocationMismatchItems());
        response.setProgressPercentage(location.getProgressPercentage());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
    public Integer getCountedItems() { return countedItems; }
    public void setCountedItems(Integer countedItems) { this.countedItems = countedItems; }
    public Integer getMatchedItems() { return matchedItems; }
    public void setMatchedItems(Integer matchedItems) { this.matchedItems = matchedItems; }
    public Integer getShortageItems() { return shortageItems; }
    public void setShortageItems(Integer shortageItems) { this.shortageItems = shortageItems; }
    public Integer getOverageItems() { return overageItems; }
    public void setOverageItems(Integer overageItems) { this.overageItems = overageItems; }
    public Integer getLocationMismatchItems() { return locationMismatchItems; }
    public void setLocationMismatchItems(Integer locationMismatchItems) { this.locationMismatchItems = locationMismatchItems; }
    public double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(double progressPercentage) { this.progressPercentage = progressPercentage; }
}
