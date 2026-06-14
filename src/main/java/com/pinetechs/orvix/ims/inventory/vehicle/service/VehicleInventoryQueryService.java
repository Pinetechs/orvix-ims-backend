package com.pinetechs.orvix.ims.inventory.vehicle.service;

import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VehicleInventoryQueryService {

    Page<VehicleInventoryItem> getTaskItems(
            Long taskId,
            Pageable pageable
    );
}