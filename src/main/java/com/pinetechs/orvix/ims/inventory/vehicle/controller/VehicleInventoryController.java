package com.pinetechs.orvix.ims.inventory.vehicle.controller;

import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.inventory.common.dto.UploadExcelResponse;
import com.pinetechs.orvix.ims.inventory.task.dto.AssignInventoryTaskStaffLocationRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.InventoryTaskAssignmentResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryItemResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryLocationResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.service.VehicleInventoryQueryService;
import com.pinetechs.orvix.ims.inventory.vehicle.service.impl.VehicleInventoryImportServiceImpl;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @GetMapping("/{taskId}/items")
    public Page<VehicleInventoryItemResponse> getTaskItems(@PathVariable Long taskId,
                                                           @RequestParam(name = "page", defaultValue = "0", required = false) int page,
                                                           @RequestParam(name = "size", defaultValue = "20", required = false) int size,
                                                           @RequestParam(name = "sortBy", defaultValue = "id", required = false) String sortBy,
                                                           @RequestParam(name = "sortOrder", defaultValue = "desc", required = false) String sortOrder,
                                                           Authentication authentication) {

        Sort sort = "asc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        User currentUser = helper.currentUser(authentication);
        return vehicleInventoryQueryService.getTaskItems(taskId, currentUser,pageable);
    }



    @GetMapping("/{taskId}/assignments")
    public List<InventoryTaskAssignmentResponse> getAssignments(@PathVariable Long taskId, Authentication authentication) {
        return vehicleInventoryQueryService.getAssignments(taskId, helper.currentUser(authentication));
    }


    @PostMapping("/{taskId}/assignments")
    public List<InventoryTaskAssignmentResponse> assignStaff(@PathVariable Long taskId, @RequestBody AssignInventoryTaskStaffLocationRequest request, Authentication authentication) {
        return vehicleInventoryQueryService.assignStaff(taskId, request, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/locations")
    public List<VehicleInventoryLocationResponse> getTaskLocations(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        User currentUser = helper.currentUser(authentication);
        return vehicleInventoryQueryService.getTaskLocations(taskId, currentUser);
    }


    @PostMapping("/{taskId}/import")
    public UploadExcelResponse importVehicleExcel(@PathVariable Long taskId, @RequestParam("file") MultipartFile file, Authentication authentication) {

        return vehicleInventoryImportService.uploadExcel(taskId, file, helper.currentUser(authentication));
    }


}