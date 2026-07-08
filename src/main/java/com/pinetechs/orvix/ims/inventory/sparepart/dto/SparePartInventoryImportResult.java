package com.pinetechs.orvix.ims.inventory.sparepart.dto;

import java.util.ArrayList;
import java.util.List;

public class SparePartInventoryImportResult {
    private Long taskId;
    private Long jobId;
    private int totalRows;
    private int importedRows;
    private int importedItems;
    private int duplicateItemLocationCount;
    private int branchCount;
    private int locationCount;
    private int brandCount;
    private List<String> branches = new ArrayList<>();
    private List<String> locations = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public boolean isSuccess() {
        return errors == null || errors.isEmpty();
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getImportedRows() { return importedRows; }
    public void setImportedRows(int importedRows) { this.importedRows = importedRows; }
    public int getImportedItems() { return importedItems; }
    public void setImportedItems(int importedItems) { this.importedItems = importedItems; }
    public int getDuplicateItemLocationCount() { return duplicateItemLocationCount; }
    public void setDuplicateItemLocationCount(int duplicateItemLocationCount) { this.duplicateItemLocationCount = duplicateItemLocationCount; }
    public int getBranchCount() { return branchCount; }
    public void setBranchCount(int branchCount) { this.branchCount = branchCount; }
    public int getLocationCount() { return locationCount; }
    public void setLocationCount(int locationCount) { this.locationCount = locationCount; }
    public int getBrandCount() { return brandCount; }
    public void setBrandCount(int brandCount) { this.brandCount = brandCount; }
    public List<String> getBranches() { return branches; }
    public void setBranches(List<String> branches) { this.branches = branches; }
    public List<String> getLocations() { return locations; }
    public void setLocations(List<String> locations) { this.locations = locations; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public String toJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"taskId\":").append(taskId).append(",");
        sb.append("\"jobId\":").append(jobId).append(",");
        sb.append("\"totalRows\":").append(totalRows).append(",");
        sb.append("\"importedRows\":").append(importedRows).append(",");
        sb.append("\"importedItems\":").append(importedItems).append(",");
        sb.append("\"duplicateItemLocationCount\":").append(duplicateItemLocationCount).append(",");
        sb.append("\"branchCount\":").append(branchCount).append(",");
        sb.append("\"locationCount\":").append(locationCount).append(",");
        sb.append("\"brandCount\":").append(brandCount).append(",");
        sb.append("\"branches\":").append(branches.toString()).append(",");
        sb.append("\"locations\":").append(locations.toString()).append(",");
        sb.append("\"errors\":").append(errors.toString());
        sb.append("}");
        return sb.toString();
    }
}
