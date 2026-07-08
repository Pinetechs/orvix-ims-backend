package com.pinetechs.orvix.ims.inventory.sparepart.service;

import com.pinetechs.orvix.ims.inventory.sparepart.dto.*;
import com.pinetechs.orvix.ims.inventory.task.dto.AssignInventoryTaskStaffBranchRequest;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SparePartInventoryQueryService {
    Page<SparePartInventoryItemResponse> getTaskItems(Long taskId, User currentUser, Pageable pageable);
    List<SparePartInventoryBranchResponse> getTaskBranches(Long taskId, User currentUser);
    List<SparePartInventoryBranchResponse> getMyAssignedBranches(Long taskId, User currentUser);
    List<SparePartInventoryLocationResponse> getBranchLocations(Long taskId, Long branchId, User currentUser);
    List<SparePartInventoryBrandResponse> getTaskBrands(Long taskId, User currentUser);
    List<SparePartInventoryAssignmentResponse> getAssignments(Long taskId, User currentUser);
    List<SparePartInventoryAssignmentResponse> assignStaff(Long taskId, AssignInventoryTaskStaffBranchRequest request, User currentUser);
}
