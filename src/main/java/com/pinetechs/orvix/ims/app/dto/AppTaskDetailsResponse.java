package com.pinetechs.orvix.ims.app.dto;

import java.util.List;

public class AppTaskDetailsResponse {

    private final AppTaskSummaryResponse task;
    private final List<AppWorkAreaResponse> workAreas;

    public AppTaskDetailsResponse(AppTaskSummaryResponse task, List<AppWorkAreaResponse> workAreas) {
        this.task = task;
        this.workAreas = workAreas;
    }

    public AppTaskSummaryResponse getTask() { return task; }
    public List<AppWorkAreaResponse> getWorkAreas() { return workAreas; }
}
