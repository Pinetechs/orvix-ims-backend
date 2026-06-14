package com.pinetechs.orvix.ims.inventory.task.dto;

public class CreateInventoryTaskRequest {

    private Long companyId;
    private String notes;

    public Long getCompanyId() {return companyId;}

    public void setCompanyId(Long companyId) {this.companyId = companyId;}

    public String getNotes() {return notes;}

    public void setNotes(String notes) {this.notes = notes;}
}