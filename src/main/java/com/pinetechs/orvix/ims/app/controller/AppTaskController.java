package com.pinetechs.orvix.ims.app.controller;

import com.pinetechs.orvix.ims.app.dto.AppTaskDetailsResponse;
import com.pinetechs.orvix.ims.app.dto.AppTasksMenuResponse;
import com.pinetechs.orvix.ims.app.dto.AppWorkAreaResponse;
import com.pinetechs.orvix.ims.app.service.AppTaskService;
import com.pinetechs.orvix.ims.app.service.AppWorkAreaService;
import com.pinetechs.orvix.ims.app.service.AppHierarchyService;
import com.pinetechs.orvix.ims.app.dto.AppHierarchyOptionResponse;
import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/app/v1/tasks")
public class AppTaskController {

    private final AppTaskService appTaskService;
    private final AppWorkAreaService appWorkAreaService;
    private final Helper helper;
    private final AppHierarchyService appHierarchyService;

    public AppTaskController(AppTaskService appTaskService, AppWorkAreaService appWorkAreaService,
                             AppHierarchyService appHierarchyService, Helper helper) {
        this.appTaskService = appTaskService;
        this.appWorkAreaService = appWorkAreaService;
        this.helper = helper;
        this.appHierarchyService = appHierarchyService;
    }

    @GetMapping("/{taskId}/work-areas/{locationId}/floors")
    public List<AppHierarchyOptionResponse> getAssetFloors(
            @PathVariable Long taskId, @PathVariable Long locationId, Authentication authentication) {
        return appHierarchyService.assetFloors(taskId, locationId, currentUser(authentication));
    }

    @GetMapping("/{taskId}/floors/{floorId}/places")
    public List<AppHierarchyOptionResponse> getAssetPlaces(
            @PathVariable Long taskId, @PathVariable Long floorId, Authentication authentication) {
        return appHierarchyService.assetPlaces(taskId, floorId, currentUser(authentication));
    }

    @GetMapping("/{taskId}/work-areas/{branchId}/locations")
    public List<AppHierarchyOptionResponse> getSparePartLocations(
            @PathVariable Long taskId, @PathVariable Long branchId, Authentication authentication) {
        return appHierarchyService.sparePartLocations(taskId, branchId, currentUser(authentication));
    }

    @GetMapping
    public AppTasksMenuResponse getMyTasks(@RequestParam(defaultValue = "false") boolean includeCompleted,
                                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
                                           Authentication authentication) {
        return appTaskService.getMyTasks(currentUser(authentication), includeCompleted ,page, size);
    }


    @GetMapping("/{taskId}")
    public AppTaskDetailsResponse getMyTask(@PathVariable Long taskId, Authentication authentication) {
        return appTaskService.getMyTask(taskId, currentUser(authentication));
    }



    @GetMapping("/{taskId}/work-areas")
    public Slice<AppWorkAreaResponse> getAssignedWorkAreas(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return appWorkAreaService.getAssignedWorkAreas(
                taskId,
                currentUser(authentication),
                page,
                size
        );
    }





    private User currentUser(Authentication authentication) {
        return helper.currentUser(authentication);
    }
}
