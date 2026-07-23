package com.pinetechs.orvix.ims.inventory.review.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CreateRecheckRequest {

    private Long assignedUserId;
    private List<Long> issueIds;
    private String instructions;
    private Boolean imageRequired;
    private LocalDateTime dueAt;

    public Long getAssignedUserId() { return assignedUserId; }
    public void setAssignedUserId(Long assignedUserId) { this.assignedUserId = assignedUserId; }
    public List<Long> getIssueIds() { return issueIds; }
    public void setIssueIds(List<Long> issueIds) { this.issueIds = issueIds; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public Boolean getImageRequired() { return imageRequired; }
    public void setImageRequired(Boolean imageRequired) { this.imageRequired = imageRequired; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
}
