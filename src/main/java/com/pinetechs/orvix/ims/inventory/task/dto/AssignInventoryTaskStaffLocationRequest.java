package com.pinetechs.orvix.ims.inventory.task.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;

import java.util.ArrayList;
import java.util.List;

public class AssignInventoryTaskStaffLocationRequest {

    private List<StaffLocationAssignmentRequest> locationAssignments = new ArrayList<>();
    private InventoryTaskStatus taskStatus;

    public List<StaffLocationAssignmentRequest> getLocationAssignments() {
        return locationAssignments;
    }

    public void setLocationAssignments(List<StaffLocationAssignmentRequest> locationAssignments) {
        this.locationAssignments = locationAssignments;
    }

    public InventoryTaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(InventoryTaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }
}