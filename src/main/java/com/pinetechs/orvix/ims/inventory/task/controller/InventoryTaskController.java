package com.pinetechs.orvix.ims.inventory.task.controller;

import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.dto.CreateInventoryTaskRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.TaskResponse;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.dto.UpdateSparePartLocationProgressModeRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.UpdateInventoryTaskScanSettingsRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.InventoryTaskReasonRequest;
import com.pinetechs.orvix.ims.inventory.task.service.impl.InventoryTaskServiceImpl;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/tasks")
public class InventoryTaskController {

    private final InventoryTaskServiceImpl inventoryTaskService;
    private final Helper helper ;

    public InventoryTaskController(InventoryTaskServiceImpl inventoryTaskService, Helper helper) {
        this.inventoryTaskService = inventoryTaskService;
        this.helper = helper;
    }

    @PostMapping
    public TaskResponse createTask(@RequestBody CreateInventoryTaskRequest request , Authentication authentication) {

        return inventoryTaskService.createTask(request, helper.currentUser(authentication));
    }



    @GetMapping("/{taskId}")
    public TaskResponse getTaskById(@PathVariable Long taskId,Authentication authentication) {
        User currentUser = helper.currentUser(authentication);
        return inventoryTaskService.getTaskById(taskId ,currentUser);
    }

    @PatchMapping("/{taskId}/spare-part-location-progress-mode")
    public TaskResponse updateSparePartLocationProgressMode(
            @PathVariable Long taskId,
            @RequestBody UpdateSparePartLocationProgressModeRequest request,
            Authentication authentication
    ) {
        return inventoryTaskService.updateSparePartLocationProgressMode(
                taskId, request, helper.currentUser(authentication));
    }

    @PatchMapping("/{taskId}/scan-settings")
    public TaskResponse updateScanSettings(
            @PathVariable Long taskId,
            @RequestBody UpdateInventoryTaskScanSettingsRequest request,
            Authentication authentication
    ) {
        return inventoryTaskService.updateScanSettings(
                taskId, request, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/eligible-staff")
    public Page<UserResponse> getEligibleStaff(
            @PathVariable Long taskId,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("firstName").ascending()
                .and(Sort.by("lastName").ascending()));
        return inventoryTaskService.getEligibleStaff(
                taskId, search, pageable, helper.currentUser(authentication));
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
    public TaskResponse markReadyToStart(@PathVariable Long taskId, Authentication authentication) {
        return inventoryTaskService.markReadyToStart(taskId, helper.currentUser(authentication));
    }


    @PostMapping("/{taskId}/start")
    public TaskResponse startTask(@PathVariable Long taskId, Authentication authentication) {
        return inventoryTaskService.startTask(taskId, helper.currentUser(authentication));
    }

    @PostMapping("/{taskId}/pause")
    public TaskResponse pauseTask(@PathVariable Long taskId, @RequestBody(required = false) InventoryTaskReasonRequest request, @RequestParam(required = false) String reason,
            Authentication authentication
    ) {
        String resolvedReason = request == null ? reason : request.getReason();
        return inventoryTaskService.pauseTask(taskId, resolvedReason, helper.currentUser(authentication));
    }

    @PostMapping("/{taskId}/resume")
    public TaskResponse resumeTask(@PathVariable Long taskId, Authentication authentication) {
        return inventoryTaskService.resumeTask(taskId, helper.currentUser(authentication));
    }

    @PostMapping("/{taskId}/review")
    public TaskResponse moveToReview(@PathVariable Long taskId, Authentication authentication) {
        return inventoryTaskService.moveToReview(taskId, helper.currentUser(authentication));
    }

    @PostMapping("/{taskId}/return-to-progress")
    public TaskResponse returnToProgress(
            @PathVariable Long taskId,
            @RequestBody(required = false) InventoryTaskReasonRequest request,
            @RequestParam(required = false) String reason,
            Authentication authentication
    ) {
        String resolvedReason = request == null ? reason : request.getReason();
        return inventoryTaskService.returnToProgress(
                taskId, resolvedReason, helper.currentUser(authentication));
    }

    @PostMapping("/{taskId}/complete")
    public TaskResponse completeTask(@PathVariable Long taskId, Authentication authentication) {
        return inventoryTaskService.completeTask(taskId, helper.currentUser(authentication));
    }

    @PostMapping("/{taskId}/cancel")
    public TaskResponse cancelTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) InventoryTaskReasonRequest request,
            @RequestParam(required = false) String reason,
            Authentication authentication
    ) {
        String resolvedReason = request == null ? reason : request.getReason();
        return inventoryTaskService.cancelTask(
                taskId, resolvedReason, helper.currentUser(authentication));
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long taskId, Authentication authentication) {
        inventoryTaskService.deleteTask(taskId, helper.currentUser(authentication));
    }
}
