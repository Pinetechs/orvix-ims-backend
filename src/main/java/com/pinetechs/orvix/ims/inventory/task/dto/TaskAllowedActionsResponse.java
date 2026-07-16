package com.pinetechs.orvix.ims.inventory.task.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;

public class TaskAllowedActionsResponse {

    private boolean editScanSettings;
    private boolean editAssignments;
    private boolean markReady;
    private boolean start;
    private boolean pause;
    private boolean resume;
    private boolean review;
    private boolean complete;
    private boolean delete;
    private boolean deleteRequiresPause;
    private boolean cancel;

    public static TaskAllowedActionsResponse from(InventoryTaskStatus status, long scanCount) {
        TaskAllowedActionsResponse response = new TaskAllowedActionsResponse();
        boolean closed = status == InventoryTaskStatus.COMPLETED || status == InventoryTaskStatus.CANCELLED;

        response.editScanSettings = !closed;
        response.editAssignments = status == InventoryTaskStatus.IMPORT_COMPLETED
                || status == InventoryTaskStatus.READY_FOR_ASSIGNMENT
                || status == InventoryTaskStatus.READY_TO_START
                || status == InventoryTaskStatus.IN_PROGRESS
                || status == InventoryTaskStatus.PAUSED;
        response.markReady = status == InventoryTaskStatus.IMPORT_COMPLETED
                || status == InventoryTaskStatus.READY_FOR_ASSIGNMENT
                || status == InventoryTaskStatus.DRAFT;
        response.start = status == InventoryTaskStatus.IMPORT_COMPLETED
                || status == InventoryTaskStatus.READY_FOR_ASSIGNMENT
                || status == InventoryTaskStatus.READY_TO_START;
        response.pause = status == InventoryTaskStatus.IN_PROGRESS;
        response.resume = status == InventoryTaskStatus.PAUSED;
        response.review = status == InventoryTaskStatus.IN_PROGRESS;
        response.complete = status == InventoryTaskStatus.UNDER_REVIEW;
        response.deleteRequiresPause = status == InventoryTaskStatus.IN_PROGRESS && scanCount < 10;
        response.delete = !closed
                && status != InventoryTaskStatus.IN_PROGRESS
                && status != InventoryTaskStatus.IMPORT_IN_PROGRESS
                && scanCount < 10;
        response.cancel = !closed
                && status != InventoryTaskStatus.IMPORT_PENDING
                && status != InventoryTaskStatus.IMPORT_IN_PROGRESS;
        return response;
    }

    public boolean isEditScanSettings() { return editScanSettings; }
    public boolean isEditAssignments() { return editAssignments; }
    public boolean isMarkReady() { return markReady; }
    public boolean isStart() { return start; }
    public boolean isPause() { return pause; }
    public boolean isResume() { return resume; }
    public boolean isReview() { return review; }
    public boolean isComplete() { return complete; }
    public boolean isDelete() { return delete; }
    public boolean isDeleteRequiresPause() { return deleteRequiresPause; }
    public boolean isCancel() { return cancel; }
}
