package com.pinetechs.orvix.ims.inventory.task.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;

public class TaskAllowedActionsResponse {

    private boolean editScanSettings;
    private boolean editAssignments;
    private boolean markReady;
    private boolean start;
    private boolean pause;
    private boolean resume;
    private boolean review;
    private boolean complete;
    private boolean returnToProgress;
    private boolean delete;
    private boolean deleteRequiresPause;
    private boolean cancel;

    public static TaskAllowedActionsResponse from(InventoryTaskStatus status, long scanCount) {
        return build(status, scanCount, true, true, true);
    }

    public static TaskAllowedActionsResponse from(
            InventoryTaskStatus status,
            InventoryDomain domain,
            long scanCount,
            User user
    ) {
        boolean canUpdate = hasPermission(user, domain, PermissionType.UPDATE);
        boolean canAssign = hasPermission(user, domain, PermissionType.ASSIGN);
        boolean canClose = hasPermission(user, domain, PermissionType.CLOSE);
        return build(status, scanCount, canUpdate, canAssign, canClose);
    }

    private static TaskAllowedActionsResponse build(
            InventoryTaskStatus status,
            long scanCount,
            boolean canUpdate,
            boolean canAssign,
            boolean canClose
    ) {
        TaskAllowedActionsResponse response = new TaskAllowedActionsResponse();
        boolean closed = status == InventoryTaskStatus.COMPLETED || status == InventoryTaskStatus.CANCELLED;

        response.editScanSettings = canUpdate && !closed;
        response.editAssignments = canAssign && (status == InventoryTaskStatus.IMPORT_COMPLETED
                || status == InventoryTaskStatus.READY_FOR_ASSIGNMENT
                || status == InventoryTaskStatus.READY_TO_START
                || status == InventoryTaskStatus.IN_PROGRESS
                || status == InventoryTaskStatus.PAUSED);
        response.markReady = canAssign && (status == InventoryTaskStatus.IMPORT_COMPLETED
                || status == InventoryTaskStatus.READY_FOR_ASSIGNMENT
                || status == InventoryTaskStatus.DRAFT);
        response.start = canUpdate && (status == InventoryTaskStatus.IMPORT_COMPLETED
                || status == InventoryTaskStatus.READY_FOR_ASSIGNMENT
                || status == InventoryTaskStatus.READY_TO_START);
        response.pause = canUpdate && status == InventoryTaskStatus.IN_PROGRESS;
        response.resume = canUpdate && status == InventoryTaskStatus.PAUSED;
        response.review = canUpdate && status == InventoryTaskStatus.IN_PROGRESS;
        response.complete = canClose && status == InventoryTaskStatus.UNDER_REVIEW;
        response.returnToProgress = canUpdate && status == InventoryTaskStatus.UNDER_REVIEW;
        response.deleteRequiresPause = canUpdate && status == InventoryTaskStatus.IN_PROGRESS && scanCount < 10;
        response.delete = canUpdate && !closed
                && status != InventoryTaskStatus.IN_PROGRESS
                && status != InventoryTaskStatus.IMPORT_IN_PROGRESS
                && scanCount < 10;
        response.cancel = canClose && !closed
                && status != InventoryTaskStatus.IMPORT_PENDING
                && status != InventoryTaskStatus.IMPORT_IN_PROGRESS;
        return response;
    }

    private static boolean hasPermission(User user, InventoryDomain domain, PermissionType type) {
        if (user == null || domain == null) return false;
        PermissionCode permission;
        if (domain == InventoryDomain.VEHICLE) {
            permission = switch (type) {
                case UPDATE -> PermissionCode.VEHICLE_TASK_UPDATE;
                case ASSIGN -> PermissionCode.VEHICLE_TASK_ASSIGN_USERS;
                case CLOSE -> PermissionCode.VEHICLE_TASK_CLOSE;
            };
        } else if (domain == InventoryDomain.ASSET) {
            permission = switch (type) {
                case UPDATE -> PermissionCode.ASSET_TASK_UPDATE;
                case ASSIGN -> PermissionCode.ASSET_TASK_ASSIGN_USERS;
                case CLOSE -> PermissionCode.ASSET_TASK_CLOSE;
            };
        } else {
            permission = switch (type) {
                case UPDATE -> PermissionCode.SPARE_PART_TASK_UPDATE;
                case ASSIGN -> PermissionCode.SPARE_PART_TASK_ASSIGN_USERS;
                case CLOSE -> PermissionCode.SPARE_PART_TASK_CLOSE;
            };
        }
        return user.hasPermission(permission);
    }

    private enum PermissionType { UPDATE, ASSIGN, CLOSE }

    public boolean isEditScanSettings() { return editScanSettings; }
    public boolean isEditAssignments() { return editAssignments; }
    public boolean isMarkReady() { return markReady; }
    public boolean isStart() { return start; }
    public boolean isPause() { return pause; }
    public boolean isResume() { return resume; }
    public boolean isReview() { return review; }
    public boolean isComplete() { return complete; }
    public boolean isReturnToProgress() { return returnToProgress; }
    public boolean isDelete() { return delete; }
    public boolean isDeleteRequiresPause() { return deleteRequiresPause; }
    public boolean isCancel() { return cancel; }
}
