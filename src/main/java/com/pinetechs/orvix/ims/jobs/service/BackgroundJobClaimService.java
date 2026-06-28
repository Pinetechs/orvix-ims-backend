package com.pinetechs.orvix.ims.jobs.service;

import com.pinetechs.orvix.ims.jobs.enums.JobStatus;
import com.pinetechs.orvix.ims.jobs.repository.BackgroundJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BackgroundJobClaimService {

    private final BackgroundJobRepository backgroundJobRepository;

    public BackgroundJobClaimService(BackgroundJobRepository backgroundJobRepository) {
        this.backgroundJobRepository = backgroundJobRepository;
    }

    @Transactional
    public int claimJobs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        return backgroundJobRepository.claimJob(ids, JobStatus.PENDING, JobStatus.RUNNING);
    }
}