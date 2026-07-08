package com.pinetechs.orvix.ims.inventory.asset.service;

import com.pinetechs.orvix.ims.inventory.asset.dto.*;
import com.pinetechs.orvix.ims.inventory.task.dto.AssignInventoryTaskStaffLocationRequest;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AssetInventoryQueryService {

    Page<AssetInventoryItemResponse> getTaskItems(Long taskId, User currentUser, Pageable pageable);

    List<AssetInventoryLocationResponse> getTaskLocations(Long taskId, User currentUser);

    List<AssetInventoryLocationResponse> getMyAssignedLocations(Long taskId, User currentUser);

    List<AssetInventoryFloorResponse> getLocationFloors(Long taskId, Long locationId, User currentUser);

    List<AssetInventoryPlaceResponse> getFloorPlaces(Long taskId, Long floorId, User currentUser);

    List<AssetInventoryCategoryResponse> getTaskCategories(Long taskId, User currentUser);

    List<AssetInventoryAssignmentResponse> getAssignments(Long taskId, User currentUser);

    List<AssetInventoryAssignmentResponse> assignStaff(Long taskId, AssignInventoryTaskStaffLocationRequest request, User currentUser);
}
