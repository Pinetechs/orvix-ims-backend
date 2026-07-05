package com.pinetechs.orvix.ims.inventory.task.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;

import java.util.ArrayList;
import java.util.List;

public class AssignInventoryTaskStaffRequest {

    private List<Long> userIds = new ArrayList<>();
    private List<StaffLocationAssignmentRequest> locationAssignments = new ArrayList<>();
    private InventoryTaskStatus taskStatus;

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

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
