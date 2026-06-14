package com.pinetechs.orvix.ims.inventory.task.controller;

import com.pinetechs.orvix.ims.inventory.task.dto.CreateInventoryTaskRequest;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskService;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryImportResult;
import com.pinetechs.orvix.ims.inventory.vehicle.service.VehicleInventoryImportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/inventory/tasks")
public class InventoryTaskController {

    private final InventoryTaskService inventoryTaskService;
    private final VehicleInventoryImportService vehicleInventoryImportService;

    public InventoryTaskController(InventoryTaskService inventoryTaskService, VehicleInventoryImportService vehicleInventoryImportService) {
        this.inventoryTaskService = inventoryTaskService;
        this.vehicleInventoryImportService = vehicleInventoryImportService;
    }

    @PostMapping("/vehicle")
    public InventoryTask createVehicleTask(
            @RequestBody CreateInventoryTaskRequest request) {

        Long currentUserId = 1L; // لاحقاً من JWT

        return inventoryTaskService.createVehicleTask(request.getCompanyId(), currentUserId, request.getNotes());
    }

    @PostMapping("/{taskId}/import")
    public VehicleInventoryImportResult importVehicleExcel(@PathVariable Long taskId, @RequestParam("file") MultipartFile file) {

        Long currentUserId = 1L; // لاحقاً من JWT

        return vehicleInventoryImportService.importExcel(taskId, file, currentUserId);
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