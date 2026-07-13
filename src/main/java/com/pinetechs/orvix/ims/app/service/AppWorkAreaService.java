package com.pinetechs.orvix.ims.app.service;

import com.pinetechs.orvix.ims.app.dto.AppWorkAreaResponse;
import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryLocation;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryLocationAssignmentRepository;
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
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppWorkAreaService {

    private static final int MAX_PAGE_SIZE = 50;

    private final AccessPolicyService accessPolicyService;
    private final VehicleInventoryLocationAssignmentRepository vehicleLocationAssignmentRepository;
    private final AssetInventoryLocationAssignmentRepository assetLocationAssignmentRepository;
    private final SparePartInventoryBranchAssignmentRepository sparePartBranchAssignmentRepository;
    private final InventoryTaskAssignmentRepository assignmentRepository;

    public AppWorkAreaService(
            AccessPolicyService accessPolicyService,
            VehicleInventoryLocationAssignmentRepository vehicleLocationAssignmentRepository,
            AssetInventoryLocationAssignmentRepository assetLocationAssignmentRepository,
            SparePartInventoryBranchAssignmentRepository sparePartBranchAssignmentRepository,
            InventoryTaskAssignmentRepository assignmentRepository
    ) {
        this.accessPolicyService = accessPolicyService;
        this.vehicleLocationAssignmentRepository = vehicleLocationAssignmentRepository;
        this.assetLocationAssignmentRepository = assetLocationAssignmentRepository;
        this.sparePartBranchAssignmentRepository = sparePartBranchAssignmentRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public Slice<AppWorkAreaResponse> getAssignedWorkAreas(Long taskId, User currentUser, int page, int size) {
        assertAppUser(currentUser);

        InventoryTask task = getAssignedVisibleTask(taskId, currentUser.getId());

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
        );

        return findAssignedWorkAreas(task, currentUser.getId(), pageable);
    }

    private InventoryTask getAssignedVisibleTask(Long taskId, Long userId) {
        InventoryTaskAssignment assignment = assignmentRepository
                .findActiveByTaskIdAndUserIdWithTaskAndCompany(taskId, userId)
                .orElseThrow(this::assignedTaskNotFound);

        InventoryTask task = assignment.getInventoryTask();

        if (!isVisibleInApp(task)) {
            throw assignedTaskNotFound();
        }

        return task;
    }

    private Slice<AppWorkAreaResponse> findAssignedWorkAreas(
            InventoryTask task,
            Long userId,
            Pageable pageable
    ) {
        if (task.getInventoryDomain() == null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Inventory task domain is not configured"
            );
        }

        return switch (task.getInventoryDomain()) {
            case VEHICLE -> vehicleLocationAssignmentRepository
                    .findActiveByTaskIdAndUserIdWithLocationSlice(
                            task.getId(),
                            userId,
                            pageable
                    )
                    .map(assignment ->
                            toVehicleWorkArea(assignment.getLocation())
                    );

            case ASSET -> assetLocationAssignmentRepository
                    .findActiveByTaskIdAndUserIdWithLocationSlice(
                            task.getId(),
                            userId,
                            pageable
                    )
                    .map(assignment ->
                            toAssetWorkArea(assignment.getLocation())
                    );

            case SPARE_PART -> sparePartBranchAssignmentRepository
                    .findActiveByTaskIdAndUserIdWithBranchSlice(
                            task.getId(),
                            userId,
                            pageable
                    )
                    .map(assignment ->
                            toSparePartWorkArea(assignment.getBranch())
                    );
        };
    }

    private void assertAppUser(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        accessPolicyService.assertCanUseApp(currentUser);
    }

    private boolean isVisibleInApp(InventoryTask task) {
        if (task == null || task.getStatus() == null) {
            return false;
        }

        return task.getStatus() == InventoryTaskStatus.READY_TO_START
                || task.getStatus() == InventoryTaskStatus.IN_PROGRESS
                || task.getStatus() == InventoryTaskStatus.PAUSED
                || task.getStatus() == InventoryTaskStatus.UNDER_REVIEW
                || task.getStatus() == InventoryTaskStatus.COMPLETED;
    }

    private BusinessException assignedTaskNotFound() {
        return new BusinessException(
                HttpStatus.NOT_FOUND,
                "Assigned inventory task not found"
        );
    }

    private AppWorkAreaResponse toVehicleWorkArea(
            VehicleInventoryLocation location
    ) {
        return new AppWorkAreaResponse(
                location.getId(),
                "LOCATION",
                location.getStoreNo(),
                location.getLocationName(),
                location.getTotalVehicles(),
                location.getProcessedVehicles(),
                location.getMatchedVehicles(),
                location.getProgressPercentage()
        );
    }

    private AppWorkAreaResponse toAssetWorkArea(
            AssetInventoryLocation location
    ) {
        return new AppWorkAreaResponse(
                location.getId(),
                "LOCATION",
                null,
                location.getLocationName(),
                location.getTotalAssets(),
                location.getProcessedAssets(),
                location.getMatchedAssets(),
                location.getProgressPercentage()
        );
    }

    private AppWorkAreaResponse toSparePartWorkArea(
            SparePartInventoryBranch branch
    ) {
        return new AppWorkAreaResponse(
                branch.getId(),
                "BRANCH",
                null,
                branch.getBranchName(),
                branch.getTotalItems(),
                branch.getCountedItems(),
                branch.getMatchedItems(),
                branch.getProgressPercentage()
        );
    }
}