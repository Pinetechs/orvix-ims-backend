package com.pinetechs.orvix.ims.app.service;

import com.pinetechs.orvix.ims.app.dto.AppTaskDetailsResponse;
import com.pinetechs.orvix.ims.app.dto.AppTaskSummaryResponse;
import com.pinetechs.orvix.ims.app.dto.AppTasksMenuResponse;
import com.pinetechs.orvix.ims.app.dto.AppWorkAreaResponse;
import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryLocation;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryLocationAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryBranch;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryBranchAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocation;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationAssignmentRepository;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppTaskService {

    private final InventoryTaskAssignmentRepository assignmentRepository;
    private final VehicleInventoryLocationAssignmentRepository vehicleLocationAssignmentRepository;
    private final AssetInventoryLocationAssignmentRepository assetLocationAssignmentRepository;
    private final SparePartInventoryBranchAssignmentRepository sparePartBranchAssignmentRepository;
    private final AccessPolicyService accessPolicyService;

    public AppTaskService(
            InventoryTaskAssignmentRepository assignmentRepository,
            VehicleInventoryLocationAssignmentRepository vehicleLocationAssignmentRepository,
            AssetInventoryLocationAssignmentRepository assetLocationAssignmentRepository,
            SparePartInventoryBranchAssignmentRepository sparePartBranchAssignmentRepository,
            AccessPolicyService accessPolicyService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.vehicleLocationAssignmentRepository = vehicleLocationAssignmentRepository;
        this.assetLocationAssignmentRepository = assetLocationAssignmentRepository;
        this.sparePartBranchAssignmentRepository = sparePartBranchAssignmentRepository;
        this.accessPolicyService = accessPolicyService;
    }

    @Transactional(readOnly = true)
    public AppTasksMenuResponse getMyTasks(User currentUser, boolean includeCompleted ,int page, int size) {
        assertAppUser(currentUser);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        AppTasksMenuResponse appTasksMenuResponse = new AppTasksMenuResponse();

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize
        );

        appTasksMenuResponse.setUserId(currentUser.getId());
        appTasksMenuResponse.setAssignedTasks(assignmentRepository.countActiveByUserId(currentUser.getId()));
        appTasksMenuResponse.setCompletedTasks(assignmentRepository.countActiveByUserIdAndStatus(currentUser.getId(), InventoryTaskStatus.COMPLETED));
        appTasksMenuResponse.setReadyToStartTasks(assignmentRepository.countActiveByUserIdAndStatus(currentUser.getId(), InventoryTaskStatus.READY_TO_START));
        appTasksMenuResponse.setInProgressTasks(assignmentRepository.countActiveByUserIdAndStatus(currentUser.getId(), InventoryTaskStatus.IN_PROGRESS));
        appTasksMenuResponse.setTasks(assignmentRepository.findActiveByUserIdWithTaskAndCompany(currentUser.getId(), getVisibleStatuses(includeCompleted),pageable).map(AppTaskSummaryResponse::from));




        return appTasksMenuResponse;

    }

    @Transactional(readOnly = true)
    public AppTaskDetailsResponse getMyTask(Long taskId, User currentUser) {
        assertAppUser(currentUser);

        InventoryTaskAssignment assignment = assignmentRepository
                .findActiveByTaskIdAndUserIdWithTaskAndCompany(taskId, currentUser.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Assigned inventory task not found"));

        InventoryTask task = assignment.getInventoryTask();
        if (!isVisibleInApp(task, true)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Assigned inventory task not found");
        }

        return new AppTaskDetailsResponse(
                AppTaskSummaryResponse.from(assignment),
                getAssignedWorkAreas(task, currentUser.getId())
        );
    }

    private List<AppWorkAreaResponse> getAssignedWorkAreas(InventoryTask task, Long userId) {
        if (task.getInventoryDomain() == InventoryDomain.VEHICLE) {
            return vehicleLocationAssignmentRepository
                    .findActiveByTaskIdAndUserIdWithLocation(task.getId(), userId)
                    .stream()
                    .map(assignment -> toVehicleWorkArea(assignment.getLocation()))
                    .toList();
        }

        if (task.getInventoryDomain() == InventoryDomain.ASSET) {
            return assetLocationAssignmentRepository
                    .findActiveByTaskIdAndUserIdWithLocation(task.getId(), userId)
                    .stream()
                    .map(assignment -> toAssetWorkArea(assignment.getLocation()))
                    .toList();
        }

        return sparePartBranchAssignmentRepository
                .findActiveByTaskIdAndUserIdWithBranch(task.getId(), userId)
                .stream()
                .map(assignment -> toSparePartWorkArea(assignment.getBranch()))
                .toList();
    }

    private AppWorkAreaResponse toVehicleWorkArea(VehicleInventoryLocation location) {
        return new AppWorkAreaResponse(
                location.getId(), "LOCATION", location.getStoreNo(), location.getLocationName(),
                location.getTotalVehicles(), location.getProcessedVehicles(),
                location.getMatchedVehicles(), location.getProgressPercentage()
        );
    }

    private AppWorkAreaResponse toAssetWorkArea(AssetInventoryLocation location) {
        return new AppWorkAreaResponse(
                location.getId(), "LOCATION", null, location.getLocationName(),
                location.getTotalAssets(), location.getProcessedAssets(),
                location.getMatchedAssets(), location.getProgressPercentage()
        );
    }

    private AppWorkAreaResponse toSparePartWorkArea(SparePartInventoryBranch branch) {
        return new AppWorkAreaResponse(
                branch.getId(), "BRANCH", null, branch.getBranchName(),
                branch.getTotalItems(), branch.getCountedItems(),
                branch.getMatchedItems(), branch.getProgressPercentage()
        );
    }

    private boolean isVisibleInApp(InventoryTask task, boolean includeCompleted) {
        if (task == null || task.getStatus() == null) {
            return false;
        }
        if (task.getStatus() == InventoryTaskStatus.COMPLETED) {
            return includeCompleted;
        }
        return task.getStatus() == InventoryTaskStatus.READY_TO_START
                || task.getStatus() == InventoryTaskStatus.IN_PROGRESS
                || task.getStatus() == InventoryTaskStatus.PAUSED
                || task.getStatus() == InventoryTaskStatus.UNDER_REVIEW;
    }


    private List<InventoryTaskStatus> getVisibleStatuses(boolean includeCompleted) {
        List<InventoryTaskStatus> statuses = new ArrayList<>();
        statuses.add(InventoryTaskStatus.READY_TO_START);
        statuses.add(InventoryTaskStatus.IN_PROGRESS);
        statuses.add(InventoryTaskStatus.PAUSED);
        statuses.add(InventoryTaskStatus.UNDER_REVIEW);
        if (includeCompleted) {
            statuses.add(InventoryTaskStatus.COMPLETED);
        }
        return statuses;
    }


    private void assertAppUser(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        accessPolicyService.assertCanUseApp(currentUser);
    }
}
