package com.pinetechs.orvix.ims.inventory.task.controller;

import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.inventory.task.dto.CreateInventoryTaskRequest;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskService;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryImportResult;
import com.pinetechs.orvix.ims.inventory.vehicle.service.VehicleInventoryImportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/inventory/tasks")
public class InventoryTaskController {

    private final InventoryTaskService inventoryTaskService;
    private final VehicleInventoryImportService vehicleInventoryImportService;
    private final Helper helper ;

    public InventoryTaskController(InventoryTaskService inventoryTaskService, VehicleInventoryImportService vehicleInventoryImportService, Helper helper) {
        this.inventoryTaskService = inventoryTaskService;
        this.vehicleInventoryImportService = vehicleInventoryImportService;
        this.helper = helper;
    }



    @PostMapping
    public InventoryTask createTask(
            @RequestBody CreateInventoryTaskRequest request , Authentication authentication) {

        return inventoryTaskService.createTask(request, helper.currentUser(authentication));
    }







    @PostMapping("/{taskId}/start")
    public InventoryTask startTask(@PathVariable Long taskId) {
        return inventoryTaskService.startTask(taskId);
    }

    @PostMapping("/{taskId}/pause")
    public InventoryTask pauseTask(@PathVariable Long taskId, @RequestParam(required = false) String reason) {
        return inventoryTaskService.pauseTask(taskId, reason);
    }

    @PostMapping("/{taskId}/resume")
    public InventoryTask resumeTask(@PathVariable Long taskId) {
        return inventoryTaskService.resumeTask(taskId);
    }

    @PostMapping("/{taskId}/review")
    public InventoryTask moveToReview(@PathVariable Long taskId) {
        return inventoryTaskService.moveToReview(taskId);
    }

    @PostMapping("/{taskId}/complete")
    public InventoryTask completeTask(@PathVariable Long taskId) {
        return inventoryTaskService.completeTask(taskId);
    }

    @PostMapping("/{taskId}/cancel")
    public InventoryTask cancelTask(@PathVariable Long taskId, @RequestParam(required = false) String reason) {
        return inventoryTaskService.cancelTask(taskId, reason);
    }
}