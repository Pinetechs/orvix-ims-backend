package com.pinetechs.orvix.ims.inventory.vehicle.controller;

import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.service.VehicleInventoryQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/vehicle")
public class VehicleInventoryController {

    private final VehicleInventoryQueryService vehicleInventoryQueryService;

    public VehicleInventoryController(VehicleInventoryQueryService vehicleInventoryQueryService
    ) {
        this.vehicleInventoryQueryService = vehicleInventoryQueryService;
    }

    @GetMapping("/tasks/{taskId}/items")
    public Page<VehicleInventoryItem> getTaskItems(@PathVariable Long taskId, Pageable pageable) {
        return vehicleInventoryQueryService.getTaskItems(taskId, pageable);
    }
}