
package com.pinetechs.orvix.ims.inventory.task.dto;

import com.pinetechs.orvix.ims.company.dto.CompanyResponse;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;

import java.time.LocalDateTime;

public class TaskResponse {

    private String id;
    private String taskName;
    private String taskNumber;
    private String description;
    private InventoryTaskStatus status;
    private InventoryDomain inventoryDomain;
    private int progress;
    private String createBy;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private Integer totalRecords ;
    private Integer processedRecords ;
    private Integer matchedRecords ;
    private Integer mismatchRecords ;
    private Long importJobId;
    private CompanyResponse company ;


    public static TaskResponse from(InventoryTask task) {
        TaskResponse response = new TaskResponse();

        response.setId(task.getId() == null ? null : task.getId().toString());
        response.setTaskName(task.getTaskName());
        response.setTaskNumber(task.getTaskNumber());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setInventoryDomain(task.getInventoryDomain());
        response.setProgress((int) Math.round(task.getProgressPercentage()));
        response.setClosedAt(task.getClosedAt());
        response.setCompany(CompanyResponse.from(task.getCompany()));
        response.setCreatedAt(task.getCreatedAt());
        response.setTotalRecords(task.getTotalRecords());
        response.setProcessedRecords(task.getProcessedRecords());
        response.setMatchedRecords(task.getMatchedRecords());
        response.setMismatchRecords( task.getProcessedRecords() - task.getMatchedRecords());
        response.setImportJobId(task.getImportJobId());
        User createdBy = task.getCreatedBy();
        if (createdBy != null) {
            String fullName = createdBy.getFullName();
            response.setCreateBy(
                    fullName == null || fullName.isBlank()
                            ? createdBy.getUsername()
                            : fullName
            );
        }

        return response;
    }

    public String getId() {
        return id;
    }

    public Integer getMismatchRecords() {
        return mismatchRecords;
    }

    public void setMismatchRecords(Integer mismatchRecords) {
        this.mismatchRecords = mismatchRecords;
    }

    public Long getImportJobId() {
        return importJobId;
    }

    public void setImportJobId(Long importJobId) {
        this.importJobId = importJobId;
    }

    public CompanyResponse getCompany() {
        return company;
    }

    public void setCompany(CompanyResponse company) {
        this.company = company;
    }

    public Integer getMatchedRecords() {
        return matchedRecords;
    }

    public void setMatchedRecords(Integer matchedRecords) {
        this.matchedRecords = matchedRecords;
    }

    public Integer getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(Integer processedRecords) {
        this.processedRecords = processedRecords;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }



    public void setId(String id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskNumber() {
        return taskNumber;
    }

    public void setTaskNumber(String taskNumber) {
        this.taskNumber = taskNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InventoryTaskStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryTaskStatus status) {
        this.status = status;
    }

    public InventoryDomain getInventoryDomain() {
        return inventoryDomain;
    }

    public void setInventoryDomain(InventoryDomain inventoryDomain) {
        this.inventoryDomain = inventoryDomain;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}