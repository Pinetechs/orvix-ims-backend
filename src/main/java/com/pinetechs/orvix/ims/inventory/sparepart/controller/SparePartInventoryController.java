package com.pinetechs.orvix.ims.inventory.sparepart.controller;

import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.inventory.common.dto.UploadExcelResponse;
import com.pinetechs.orvix.ims.inventory.sparepart.dto.*;
import com.pinetechs.orvix.ims.inventory.sparepart.service.SparePartInventoryImportService;
import com.pinetechs.orvix.ims.inventory.sparepart.service.SparePartInventoryQueryService;
import com.pinetechs.orvix.ims.inventory.sparepart.service.SparePartInventoryScanService;
import com.pinetechs.orvix.ims.inventory.task.dto.AssignInventoryTaskStaffBranchRequest;
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
@RequestMapping("/api/inventory/spare-part")
public class SparePartInventoryController {

    private final SparePartInventoryImportService importService;
    private final SparePartInventoryQueryService queryService;
    private final SparePartInventoryScanService scanService;
    private final Helper helper;

    public SparePartInventoryController(
            SparePartInventoryImportService importService,
            SparePartInventoryQueryService queryService,
            SparePartInventoryScanService scanService,
            Helper helper
    ) {
        this.importService = importService;
        this.queryService = queryService;
        this.scanService = scanService;
        this.helper = helper;
    }

    @PostMapping("/{taskId}/import")
    public UploadExcelResponse importSparePartExcel(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return importService.uploadExcel(taskId, file, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/items")
    public Page<SparePartInventoryItemResponse> getTaskItems(
            @PathVariable Long taskId,
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "20", required = false) int size,
            @RequestParam(name = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "desc", required = false) String sortOrder,
            Authentication authentication
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        User currentUser = helper.currentUser(authentication);
        return queryService.getTaskItems(taskId, currentUser, pageable);
    }

    @GetMapping("/{taskId}/branches")
    public List<SparePartInventoryBranchResponse> getTaskBranches(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return queryService.getTaskBranches(taskId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/my-branches")
    public List<SparePartInventoryBranchResponse> getMyAssignedBranches(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return queryService.getMyAssignedBranches(taskId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/branches/{branchId}/locations")
    public List<SparePartInventoryLocationResponse> getBranchLocations(
            @PathVariable Long taskId,
            @PathVariable Long branchId,
            Authentication authentication
    ) {
        return queryService.getBranchLocations(taskId, branchId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/brands")
    public List<SparePartInventoryBrandResponse> getTaskBrands(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return queryService.getTaskBrands(taskId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/assignments")
    public List<SparePartInventoryAssignmentResponse> getAssignments(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return queryService.getAssignments(taskId, helper.currentUser(authentication));
    }

    @PostMapping("/{taskId}/assignments")
    public List<SparePartInventoryAssignmentResponse> assignStaff(@PathVariable Long taskId, @RequestBody AssignInventoryTaskStaffBranchRequest request, Authentication authentication) {
        return queryService.assignStaff(taskId, request, helper.currentUser(authentication));
    }

    @PostMapping("/{taskId}/scan")
    public SparePartInventoryScanResponse scan(
            @PathVariable Long taskId,
            @RequestBody SparePartInventoryScanRequest request,
            Authentication authentication
    ) {
        return scanService.scan(taskId, request, helper.currentUser(authentication));
    }
}
