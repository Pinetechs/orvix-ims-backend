package com.pinetechs.orvix.ims.inventory.sparepart.dto;

import java.util.ArrayList;
import java.util.List;

public class StaffBranchAssignmentRequest {

    private Long userId;
    private List<Long> branchIds = new ArrayList<>();


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Long> getBranchIds() {
        return branchIds;
    }

    public void setBranchIds(List<Long> branchIds) {
        this.branchIds = branchIds;
    }


}