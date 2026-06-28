package com.pinetechs.orvix.ims.inventory.vehicle.dto;

import java.util.ArrayList;
import java.util.List;

public class VehicleInventoryImportResult {

    private Long taskId;
    private Long jobId;
    private int importedRows;

    private int totalRows;
    private int importedItems;
    private int duplicatedVinCount;

    private int locationCount;
    private int storeNoCount;

    private List<String> locations = new ArrayList<>();
    private List<String> storeNos = new ArrayList<>();

    private List<String> errors = new ArrayList<>();

    public boolean isSuccess() {
        return errors == null || errors.isEmpty();
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getImportedItems() {
        return importedItems;
    }

    public void setImportedItems(int importedItems) {
        this.importedItems = importedItems;
    }

    public int getDuplicatedVinCount() {
        return duplicatedVinCount;
    }

    public void setDuplicatedVinCount(int duplicatedVinCount) {
        this.duplicatedVinCount = duplicatedVinCount;
    }

    public int getLocationCount() {
        return locationCount;
    }

    public void setLocationCount(int locationCount) {
        this.locationCount = locationCount;
    }

    public int getStoreNoCount() {
        return storeNoCount;
    }

    public void setStoreNoCount(int storeNoCount) {
        this.storeNoCount = storeNoCount;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public List<String> getStoreNos() {
        return storeNos;
    }

    public void setStoreNos(List<String> storeNos) {
        this.storeNos = storeNos;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public int getImportedRows() {
        return importedRows;
    }

    public void setImportedRows(int importedRows) {
        this.importedRows = importedRows;
    }

    public String toJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"taskId\":").append(taskId).append(",");
        sb.append("\"jobId\":").append(jobId).append(",");
        sb.append("\"totalRows\":").append(totalRows).append(",");
        sb.append("\"importedItems\":").append(importedItems).append(",");
        sb.append("\"duplicatedVinCount\":").append(duplicatedVinCount).append(",");
        sb.append("\"locationCount\":").append(locationCount).append(",");
        sb.append("\"storeNoCount\":").append(storeNoCount).append(",");
        sb.append("\"locations\":").append(locations.toString()).append(",");
        sb.append("\"storeNos\":").append(storeNos.toString()).append(",");
        sb.append("\"importedRows\":").append(importedRows).append(",");
        sb.append("\"errors\":").append(errors.toString());
        sb.append("}");
        return sb.toString();
    }


}