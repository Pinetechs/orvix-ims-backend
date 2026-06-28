package com.pinetechs.orvix.ims.inventory.vehicle.controller;

import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.inventory.common.dto.UploadExcelResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryImportResult;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.service.VehicleInventoryQueryService;
import com.pinetechs.orvix.ims.inventory.vehicle.service.impl.VehicleInventoryImportServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/inventory/vehicle")
public class VehicleInventoryController {

    private final VehicleInventoryQueryService vehicleInventoryQueryService;
    private final Helper helper ;
    private final VehicleInventoryImportServiceImpl vehicleInventoryImportService;

    public VehicleInventoryController(VehicleInventoryQueryService vehicleInventoryQueryService,
                                      Helper helper, VehicleInventoryImportServiceImpl vehicleInventoryImportService) {
        this.vehicleInventoryQueryService = vehicleInventoryQueryService;
        this.helper = helper;
        this.vehicleInventoryImportService = vehicleInventoryImportService;
    }

    @GetMapping("/tasks/{taskId}/items")
    public Page<VehicleInventoryItem> getTaskItems(@PathVariable Long taskId, Pageable pageable) {
        return vehicleInventoryQueryService.getTaskItems(taskId, pageable);
    }


    @PostMapping("/{taskId}/import")
    public UploadExcelResponse importVehicleExcel(@PathVariable Long taskId, @RequestParam("file") MultipartFile file, Authentication authentication) {

        return vehicleInventoryImportService.uploadExcel(taskId, file, helper.currentUser(authentication));
    }


}