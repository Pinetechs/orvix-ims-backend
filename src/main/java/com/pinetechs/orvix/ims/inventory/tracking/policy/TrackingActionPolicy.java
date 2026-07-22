package com.pinetechs.orvix.ims.inventory.tracking.policy;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import org.springframework.stereotype.Component;

@Component
public class TrackingActionPolicy {

    public TrackingResponses.AllowedActions allowedActions(InventoryTask task, User user) {
        InventoryTaskStatus status = task.getStatus();
        boolean update = hasPermission(user, task.getInventoryDomain(), ActionPermission.UPDATE);
        boolean assign = hasPermission(user, task.getInventoryDomain(), ActionPermission.ASSIGN);
        boolean close = hasPermission(user, task.getInventoryDomain(), ActionPermission.CLOSE);
        boolean closed = status == InventoryTaskStatus.COMPLETED
                || status == InventoryTaskStatus.CANCELLED;

        return new TrackingResponses.AllowedActions(
                update && !closed,
                assign && canEditAssignments(status),
                assign && canMarkReady(status),
                update && canStart(status),
                update && status == InventoryTaskStatus.IN_PROGRESS,
                update && status == InventoryTaskStatus.PAUSED,
                update && status == InventoryTaskStatus.IN_PROGRESS,
                update && status == InventoryTaskStatus.UNDER_REVIEW,
                close && status == InventoryTaskStatus.UNDER_REVIEW,
                close && canCancel(status)
        );
    }

    private boolean canEditAssignments(InventoryTaskStatus status) {
        return status == InventoryTaskStatus.IMPORT_COMPLETED
                || status == InventoryTaskStatus.READY_FOR_ASSIGNMENT
                || status == InventoryTaskStatus.READY_TO_START
                || status == InventoryTaskStatus.IN_PROGRESS
                || status == InventoryTaskStatus.PAUSED;
    }

    private boolean canMarkReady(InventoryTaskStatus status) {
        return status == InventoryTaskStatus.IMPORT_COMPLETED
                || status == InventoryTaskStatus.READY_FOR_ASSIGNMENT
                || status == InventoryTaskStatus.DRAFT;
    }

    private boolean canStart(InventoryTaskStatus status) {
        return status == InventoryTaskStatus.IMPORT_COMPLETED
                || status == InventoryTaskStatus.READY_FOR_ASSIGNMENT
                || status == InventoryTaskStatus.READY_TO_START;
    }

    private boolean canCancel(InventoryTaskStatus status) {
        return status != InventoryTaskStatus.COMPLETED
                && status != InventoryTaskStatus.CANCELLED
                && status != InventoryTaskStatus.IMPORT_PENDING
                && status != InventoryTaskStatus.IMPORT_IN_PROGRESS;
    }

    private boolean hasPermission(User user, InventoryDomain domain, ActionPermission action) {
        PermissionCode permission = permissionFor(domain, action);
        return user != null && user.hasPermission(permission);
    }

    private PermissionCode permissionFor(InventoryDomain domain, ActionPermission action) {
        return switch (domain) {
            case VEHICLE -> switch (action) {
                case UPDATE -> PermissionCode.VEHICLE_TASK_UPDATE;
                case ASSIGN -> PermissionCode.VEHICLE_TASK_ASSIGN_USERS;
                case CLOSE -> PermissionCode.VEHICLE_TASK_CLOSE;
            };
            case ASSET -> switch (action) {
                case UPDATE -> PermissionCode.ASSET_TASK_UPDATE;
                case ASSIGN -> PermissionCode.ASSET_TASK_ASSIGN_USERS;
                case CLOSE -> PermissionCode.ASSET_TASK_CLOSE;
            };
            case SPARE_PART -> switch (action) {
                case UPDATE -> PermissionCode.SPARE_PART_TASK_UPDATE;
                case ASSIGN -> PermissionCode.SPARE_PART_TASK_ASSIGN_USERS;
                case CLOSE -> PermissionCode.SPARE_PART_TASK_CLOSE;
            };
        };
    }

    private enum ActionPermission {
        UPDATE,
        ASSIGN,
        CLOSE
    }
}
