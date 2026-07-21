package com.pinetechs.orvix.ims.dashboard.controller;

import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.dashboard.dto.DashboardOverviewResponse;
import com.pinetechs.orvix.ims.dashboard.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final Helper helper;

    public DashboardController(DashboardService dashboardService, Helper helper) {
        this.dashboardService = dashboardService;
        this.helper = helper;
    }

    @GetMapping("/overview")
    public DashboardOverviewResponse getOverview(Authentication authentication) {
        return dashboardService.getOverview(helper.currentUser(authentication));
    }
}
