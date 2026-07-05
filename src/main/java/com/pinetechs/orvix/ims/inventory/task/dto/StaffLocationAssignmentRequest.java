package com.pinetechs.orvix.ims.inventory.task.dto;

import java.util.ArrayList;
import java.util.List;

public class StaffLocationAssignmentRequest {

    private Long userId;
    private List<Long> locationIds = new ArrayList<>();
    private String notes;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Long> getLocationIds() {
        return locationIds;
    }

    public void setLocationIds(List<Long> locationIds) {
        this.locationIds = locationIds;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
