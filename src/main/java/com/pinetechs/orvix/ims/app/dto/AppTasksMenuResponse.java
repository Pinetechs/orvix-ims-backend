package com.pinetechs.orvix.ims.app.dto;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.parameters.P;

public class AppTasksMenuResponse {
   private Long userId;
    private Long assignedTasks;
   private Long readyToStartTasks;
   private Long inProgressTasks;
   private Long completedTasks;
   private Slice<AppTaskSummaryResponse> tasks;


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAssignedTasks() {
        return assignedTasks;
    }

    public void setAssignedTasks(Long assignedTasks) {
        this.assignedTasks = assignedTasks;
    }

    public Long getReadyToStartTasks() {
        return readyToStartTasks;
    }

    public void setReadyToStartTasks(Long readyToStartTasks) {
        this.readyToStartTasks = readyToStartTasks;
    }

    public Long getInProgressTasks() {
        return inProgressTasks;
    }

    public void setInProgressTasks(Long inProgressTasks) {
        this.inProgressTasks = inProgressTasks;
    }

    public Slice<AppTaskSummaryResponse> getTasks() {
        return tasks;
    }

    public Long getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(Long completedTasks) {
        this.completedTasks = completedTasks;
    }

    public void setTasks(Slice<AppTaskSummaryResponse> tasks) {
        this.tasks = tasks;
    }
}
