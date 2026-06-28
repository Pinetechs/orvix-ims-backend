package com.pinetechs.orvix.ims.inventory.task.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;

public class CreateInventoryTaskRequest {

    private Long companyId;
    private String notes;
    private String taskName;
    private String description;
    private String inventoryDomain;


    public Long getCompanyId() {return companyId;}

    public void setCompanyId(Long companyId) {this.companyId = companyId;}

    public String getNotes() {return notes;}

    public String getTaskName() {
        return taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getInventoryDomain() {
        return inventoryDomain;
    }

    public void setInventoryDomain(String inventoryDomain) {
        this.inventoryDomain = inventoryDomain;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public void setNotes(String notes) {this.notes = notes;}
}