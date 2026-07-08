package com.pinetechs.orvix.ims.inventory.task.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.sparepart.dto.StaffBranchAssignmentRequest;

import java.util.ArrayList;
import java.util.List;

public class AssignInventoryTaskStaffBranchRequest {

    private List<StaffBranchAssignmentRequest> branchAssignments = new ArrayList<>();
    private InventoryTaskStatus taskStatus;

    public List<StaffBranchAssignmentRequest> getBranchAssignments() {
        return branchAssignments;
    }

    public void setBranchAssignments(List<StaffBranchAssignmentRequest> branchAssignments) {
        this.branchAssignments = branchAssignments;
    }

    public InventoryTaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(InventoryTaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }
}