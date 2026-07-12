package com.pinetechs.orvix.ims.app.controller;

import com.pinetechs.orvix.ims.app.dto.AppTaskDetailsResponse;
import com.pinetechs.orvix.ims.app.dto.AppTaskSummaryResponse;
import com.pinetechs.orvix.ims.app.dto.AppTasksMenuResponse;
import com.pinetechs.orvix.ims.app.service.AppTaskService;
import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.user.entity.User;
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
    private final Helper helper;

    public AppTaskController(AppTaskService appTaskService, Helper helper) {
        this.appTaskService = appTaskService;
        this.helper = helper;
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

    private User currentUser(Authentication authentication) {
        return helper.currentUser(authentication);
    }
}
