package com.pinetechs.orvix.ims.jobs.controller;


import com.pinetechs.orvix.ims.jobs.dto.BackgroundJobResponse;
import com.pinetechs.orvix.ims.jobs.entity.BackgroundJob;
import com.pinetechs.orvix.ims.jobs.service.BackgroundJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/background-jobs")
public class BackgroundJobsController {

    @Autowired
    private BackgroundJobService backgroundJobService;

    @GetMapping("/{id}")
    public BackgroundJobResponse getBackgroundJobById(@PathVariable Long id) {
        return backgroundJobService.getJobById(id);
    }

}
