package com.pinetechs.orvix.ims.inventory.asset.dto;

import java.util.ArrayList;
import java.util.List;

public class AssetInventoryImportResult {

    private Long taskId;
    private Long jobId;
    private int totalRows;
    private int importedRows;
    private int importedItems;
    private int duplicatedBarcodeCount;
    private int locationCount;
    private int floorCount;
    private int placeCount;
    private int categoryCount;
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
    public int getDuplicatedBarcodeCount() { return duplicatedBarcodeCount; }
    public void setDuplicatedBarcodeCount(int duplicatedBarcodeCount) { this.duplicatedBarcodeCount = duplicatedBarcodeCount; }
    public int getLocationCount() { return locationCount; }
    public void setLocationCount(int locationCount) { this.locationCount = locationCount; }
    public int getFloorCount() { return floorCount; }
    public void setFloorCount(int floorCount) { this.floorCount = floorCount; }
    public int getPlaceCount() { return placeCount; }
    public void setPlaceCount(int placeCount) { this.placeCount = placeCount; }
    public int getCategoryCount() { return categoryCount; }
    public void setCategoryCount(int categoryCount) { this.categoryCount = categoryCount; }
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
        sb.append("\"duplicatedBarcodeCount\":").append(duplicatedBarcodeCount).append(",");
        sb.append("\"locationCount\":").append(locationCount).append(",");
        sb.append("\"floorCount\":").append(floorCount).append(",");
        sb.append("\"placeCount\":").append(placeCount).append(",");
        sb.append("\"categoryCount\":").append(categoryCount).append(",");
        sb.append("\"locations\":").append(locations.toString()).append(",");
        sb.append("\"errors\":").append(errors.toString());
        sb.append("}");
        return sb.toString();
    }
}
