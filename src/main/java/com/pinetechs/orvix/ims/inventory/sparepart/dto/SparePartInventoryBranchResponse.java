package com.pinetechs.orvix.ims.inventory.sparepart.dto;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryBranch;

public class SparePartInventoryBranchResponse {
    private Long id;
    private Long taskId;
    private String branchName;
    private Integer totalItems;
    private Integer countedItems;
    private Integer matchedItems;
    private Integer shortageItems;
    private Integer overageItems;
    private Integer locationMismatchItems;
    private double progressPercentage;

    public static SparePartInventoryBranchResponse from(SparePartInventoryBranch branch) {
        SparePartInventoryBranchResponse response = new SparePartInventoryBranchResponse();
        response.setId(branch.getId());
        response.setTaskId(branch.getInventoryTask() == null ? null : branch.getInventoryTask().getId());
        response.setBranchName(branch.getBranchName());
        response.setTotalItems(branch.getTotalItems());
        response.setCountedItems(branch.getCountedItems());
        response.setMatchedItems(branch.getMatchedItems());
        response.setShortageItems(branch.getShortageItems());
        response.setOverageItems(branch.getOverageItems());
        response.setLocationMismatchItems(branch.getLocationMismatchItems());
        response.setProgressPercentage(branch.getProgressPercentage());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
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
