package com.pinetechs.orvix.ims.app.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AppTaskSummaryResponse {

    private Long id;
    private Long assignmentId;
    private String taskNumber;
    private String taskName;
    private String description;
    private InventoryDomain inventoryDomain;
    private InventoryTaskStatus status;
    private Long companyId;
    private String companyCode;
    private String companyName;
    private LocalDate startDate;
    private Integer totalRecords;
    private Integer processedRecords;
    private Integer progress;
    private LocalDateTime assignedAt;

    public static AppTaskSummaryResponse from(InventoryTaskAssignment assignment) {
        InventoryTask task = assignment.getInventoryTask();
        AppTaskSummaryResponse response = new AppTaskSummaryResponse();
        response.id = task.getId();
        response.assignmentId = assignment.getId();
        response.taskNumber = task.getTaskNumber();
        response.taskName = task.getTaskName();
        response.description = task.getDescription();
        response.inventoryDomain = task.getInventoryDomain();
        response.status = task.getStatus();
        response.startDate = task.getStartDate();
        response.totalRecords = valueOrZero(task.getTotalRecords());
        response.processedRecords = valueOrZero(task.getProcessedRecords());
             response.progress = (int) Math.round(task.getProgressPercentage());
        response.assignedAt = assignment.getAssignedAt();
        if (task.getCompany() != null) {
            response.companyId = task.getCompany().getId();
            response.companyCode = task.getCompany().getCode();
            response.companyName = task.getCompany().getName();
        }
        return response;
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    public Long getId() { return id; }
    public Long getAssignmentId() { return assignmentId; }
    public String getTaskNumber() { return taskNumber; }
    public String getTaskName() { return taskName; }
    public String getDescription() { return description; }
    public InventoryDomain getInventoryDomain() { return inventoryDomain; }
    public InventoryTaskStatus getStatus() { return status; }
    public Long getCompanyId() { return companyId; }
    public String getCompanyCode() { return companyCode; }
    public String getCompanyName() { return companyName; }
    public LocalDate getStartDate() { return startDate; }
    public Integer getTotalRecords() { return totalRecords; }
    public Integer getProcessedRecords() { return processedRecords; }

    public Integer getProgress() { return progress; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
}
