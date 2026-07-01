package com.pinetechs.orvix.ims.inventory.vehicle.service;

import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryItemResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VehicleInventoryQueryService {

    Page<VehicleInventoryItemResponse> getTaskItems(
            Long taskId,
            User currentUser,
            Pageable pageable
    );
}