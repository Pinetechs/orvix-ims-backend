package com.pinetechs.orvix.ims.inventory.asset.controller;

import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.inventory.asset.dto.*;
import com.pinetechs.orvix.ims.inventory.asset.service.AssetInventoryImportService;
import com.pinetechs.orvix.ims.inventory.asset.service.AssetInventoryQueryService;
import com.pinetechs.orvix.ims.inventory.asset.service.AssetInventoryScanService;
import com.pinetechs.orvix.ims.inventory.common.dto.UploadExcelResponse;
import com.pinetechs.orvix.ims.inventory.task.dto.AssignInventoryTaskStaffLocationRequest;
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
@RequestMapping("/api/inventory/asset")
public class AssetInventoryController {

    private final AssetInventoryImportService assetInventoryImportService;
    private final AssetInventoryQueryService assetInventoryQueryService;
    private final AssetInventoryScanService assetInventoryScanService;
    private final Helper helper;

    public AssetInventoryController(
            AssetInventoryImportService assetInventoryImportService,
            AssetInventoryQueryService assetInventoryQueryService,
            AssetInventoryScanService assetInventoryScanService,
            Helper helper
    ) {
        this.assetInventoryImportService = assetInventoryImportService;
        this.assetInventoryQueryService = assetInventoryQueryService;
        this.assetInventoryScanService = assetInventoryScanService;
        this.helper = helper;
    }

    @PostMapping("/{taskId}/import")
    public UploadExcelResponse importAssetExcel(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return assetInventoryImportService.uploadExcel(taskId, file, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/items")
    public Page<AssetInventoryItemResponse> getTaskItems(
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
        return assetInventoryQueryService.getTaskItems(taskId, currentUser, pageable);
    }

    @GetMapping("/{taskId}/locations")
    public List<AssetInventoryLocationResponse> getTaskLocations(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return assetInventoryQueryService.getTaskLocations(taskId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/my-locations")
    public List<AssetInventoryLocationResponse> getMyAssignedLocations(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return assetInventoryQueryService.getMyAssignedLocations(taskId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/locations/{locationId}/floors")
    public List<AssetInventoryFloorResponse> getLocationFloors(
            @PathVariable Long taskId,
            @PathVariable Long locationId,
            Authentication authentication
    ) {
        return assetInventoryQueryService.getLocationFloors(taskId, locationId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/floors/{floorId}/places")
    public List<AssetInventoryPlaceResponse> getFloorPlaces(
            @PathVariable Long taskId,
            @PathVariable Long floorId,
            Authentication authentication
    ) {
        return assetInventoryQueryService.getFloorPlaces(taskId, floorId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/categories")
    public List<AssetInventoryCategoryResponse> getTaskCategories(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return assetInventoryQueryService.getTaskCategories(taskId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/assignments")
    public List<AssetInventoryAssignmentResponse> getAssignments(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return assetInventoryQueryService.getAssignments(taskId, helper.currentUser(authentication));
    }

    @PostMapping("/{taskId}/assignments")
    public List<AssetInventoryAssignmentResponse> assignStaff(
            @PathVariable Long taskId,
            @RequestBody AssignInventoryTaskStaffLocationRequest request,
            Authentication authentication
    ) {
        return assetInventoryQueryService.assignStaff(taskId, request, helper.currentUser(authentication));
    }

    @PostMapping("/{taskId}/scan")
    public AssetInventoryScanResponse scan(
            @PathVariable Long taskId,
            @RequestBody AssetInventoryScanRequest request,
            Authentication authentication
    ) {
        return assetInventoryScanService.scan(taskId, request, helper.currentUser(authentication));
    }
}
