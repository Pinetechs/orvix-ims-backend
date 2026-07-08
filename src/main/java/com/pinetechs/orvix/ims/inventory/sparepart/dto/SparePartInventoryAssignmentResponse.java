package com.pinetechs.orvix.ims.inventory.sparepart.dto;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryBranchAssignment;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import com.pinetechs.orvix.ims.user.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SparePartInventoryAssignmentResponse {
    private Long id;
    private Long taskId;
    private Long userId;
    private String username;
    private String fullName;
    private Boolean active;
    private LocalDateTime assignedAt;
    private int assignedBranchCount;
    private List<SparePartInventoryBranchResponse> branches = new ArrayList<>();

    public static SparePartInventoryAssignmentResponse from(
            InventoryTaskAssignment assignment,
            List<SparePartInventoryBranchAssignment> branchAssignments
    ) {
        SparePartInventoryAssignmentResponse response = new SparePartInventoryAssignmentResponse();
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

        if (branchAssignments != null) {
            List<SparePartInventoryBranchResponse> branchResponses = branchAssignments.stream()
                    .filter(branchAssignment -> Boolean.TRUE.equals(branchAssignment.getActive()))
                    .map(SparePartInventoryBranchAssignment::getBranch)
                    .map(SparePartInventoryBranchResponse::from)
                    .toList();
            response.setBranches(branchResponses);
            response.setAssignedBranchCount(branchResponses.size());
        }

        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public int getAssignedBranchCount() { return assignedBranchCount; }
    public void setAssignedBranchCount(int assignedBranchCount) { this.assignedBranchCount = assignedBranchCount; }
    public List<SparePartInventoryBranchResponse> getBranches() { return branches; }
    public void setBranches(List<SparePartInventoryBranchResponse> branches) { this.branches = branches; }
}
