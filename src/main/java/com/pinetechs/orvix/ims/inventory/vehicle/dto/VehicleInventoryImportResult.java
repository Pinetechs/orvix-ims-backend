package com.pinetechs.orvix.ims.inventory.vehicle.dto;

import java.util.ArrayList;
import java.util.List;

public class VehicleInventoryImportResult {

    private Long taskId;
    private Integer totalRows = 0;
    private Integer importedItems = 0;
    private Integer duplicatedVinCount = 0;
    private Integer locationCount = 0;
    private List<String> errors = new ArrayList<>();

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }


    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getImportedItems() {
        return importedItems;
    }

    public void setImportedItems(Integer importedItems) {
        this.importedItems = importedItems;
    }

    public Integer getDuplicatedVinCount() {
        return duplicatedVinCount;
    }

    public void setDuplicatedVinCount(Integer duplicatedVinCount) {
        this.duplicatedVinCount = duplicatedVinCount;
    }

    public Integer getLocationCount() {
        return locationCount;
    }

    public void setLocationCount(Integer locationCount) {
        this.locationCount = locationCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}