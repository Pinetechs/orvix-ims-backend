package com.pinetechs.orvix.ims.inventory.task.controller;

import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.dto.AssignInventoryTaskStaffRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.CreateInventoryTaskRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.InventoryTaskAssignmentResponse;
import com.pinetechs.orvix.ims.inventory.task.dto.TaskResponse;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskService;
import com.pinetechs.orvix.ims.inventory.task.service.impl.InventoryTaskServiceImpl;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryImportResult;
import com.pinetechs.orvix.ims.inventory.vehicle.service.VehicleInventoryImportService;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import com.pinetechs.orvix.ims.user.enums.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/tasks")
public class InventoryTaskController {

    private final InventoryTaskServiceImpl inventoryTaskService;
    private final VehicleInventoryImportService vehicleInventoryImportService;
    private final Helper helper ;

    public InventoryTaskController(InventoryTaskServiceImpl inventoryTaskService, VehicleInventoryImportService vehicleInventoryImportService, Helper helper) {
        this.inventoryTaskService = inventoryTaskService;
        this.vehicleInventoryImportService = vehicleInventoryImportService;
        this.helper = helper;
    }

    @PostMapping
    public InventoryTask createTask(
            @RequestBody CreateInventoryTaskRequest request , Authentication authentication) {

        return inventoryTaskService.createTask(request, helper.currentUser(authentication));
    }



    @GetMapping("/{taskId}")
    public TaskResponse getTaskById(@PathVariable Long taskId,Authentication authentication) {
        User currentUser = helper.currentUser(authentication);
        return inventoryTaskService.getTaskById(taskId ,currentUser);
    }



    @GetMapping
    public Page<TaskResponse> getTasks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "20", required = false) int size,
            @RequestParam(name = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "desc", required = false) String sortOrder,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) InventoryTaskStatus status,
            @RequestParam(name = "inventoryDomain", required = false) String inventoryDomains,
            @RequestParam(name = "companyId", required = false) Long companyId,
            Authentication authentication
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return inventoryTaskService.getTasks(
                pageable,
                helper.currentUser(authentication),
                search,
                companyId,
                status,
                inventoryDomains
        );
    }







    @PostMapping("/{taskId}/ready-to-start")
    public InventoryTask markReadyToStart(@PathVariable Long taskId, Authentication authentication) {
        return inventoryTaskService.markReadyToStart(taskId, helper.currentUser(authentication));
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