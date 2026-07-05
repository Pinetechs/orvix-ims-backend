package com.pinetechs.orvix.ims.inventory.task.dto;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryLocationResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocationAssignment;
import com.pinetechs.orvix.ims.user.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InventoryTaskAssignmentResponse {

    private Long id;
    private Long taskId;
    private Long userId;
    private String username;
    private String fullName;
    private Boolean active;
    private LocalDateTime assignedAt;
    private int assignedLocationCount;
    private List<VehicleInventoryLocationResponse> locations = new ArrayList<>();

    public static InventoryTaskAssignmentResponse from(
            InventoryTaskAssignment assignment,
            List<VehicleInventoryLocationAssignment> locationAssignments
    ) {
        InventoryTaskAssignmentResponse response = new InventoryTaskAssignmentResponse();
        response.setId(assignment.getId());
        response.setTaskId(assignment.getInventoryTask() == null ? null : assignment.getInventoryTask().getId());
        response.setActive(assignment.getActive());
        response.setAssignedAt(assignment.getAssignedAt());

        User user = assignment.getUser();
        if (user != null) {
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setFullName(user.getFullName());
        }

        if (locationAssignments != null) {
            List<VehicleInventoryLocationResponse> locationResponses = locationAssignments.stream()
                    .filter(locationAssignment -> Boolean.TRUE.equals(locationAssignment.getActive()))
                    .map(VehicleInventoryLocationAssignment::getLocation)
                    .map(VehicleInventoryLocationResponse::from)
                    .toList();

            response.setLocations(locationResponses);
            response.setAssignedLocationCount(locationResponses.size());
        }

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public int getAssignedLocationCount() {
        return assignedLocationCount;
    }

    public void setAssignedLocationCount(int assignedLocationCount) {
        this.assignedLocationCount = assignedLocationCount;
    }

    public List<VehicleInventoryLocationResponse> getLocations() {
        return locations;
    }

    public void setLocations(List<VehicleInventoryLocationResponse> locations) {
        this.locations = locations;
    }
}
