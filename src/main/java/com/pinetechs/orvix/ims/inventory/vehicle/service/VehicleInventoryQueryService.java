package com.pinetechs.orvix.ims.inventory.vehicle.service;

import com.pinetechs.orvix.ims.inventory.task.dto.AssignInventoryTaskStaffRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.InventoryTaskAssignmentResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryItemResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryLocationResponse;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VehicleInventoryQueryService {

    Page<VehicleInventoryItemResponse> getTaskItems(
            Long taskId,
            User currentUser,
            Pageable pageable
    );

    List<VehicleInventoryLocationResponse> getTaskLocations(Long taskId, User currentUser);
    List<InventoryTaskAssignmentResponse> getAssignments(Long taskId, User currentUser);

     List<InventoryTaskAssignmentResponse> assignStaff(Long taskId, AssignInventoryTaskStaffRequest request, User currentUser) ;



}
