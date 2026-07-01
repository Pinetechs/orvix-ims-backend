package com.pinetechs.orvix.ims.jobs.service;


import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.jobs.dto.BackgroundJobResponse;
import com.pinetechs.orvix.ims.jobs.entity.BackgroundJob;
import com.pinetechs.orvix.ims.jobs.repository.BackgroundJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BackgroundJobService {

    private final BackgroundJobRepository backgroundJobRepository;


    public BackgroundJobService(BackgroundJobRepository backgroundJobRepository) {
        this.backgroundJobRepository = backgroundJobRepository;
    }

    public BackgroundJobResponse getJobById(Long jobId) {
       BackgroundJob  job=  backgroundJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,"Background job not found with id: " + jobId));

        return new BackgroundJobResponse(job.getId(), job.getJobName(), job.getRelatedId(), job.getProgress(), job.getStatus(),job.getErrorMessage(),job.getMessage(),job.getResult());
    }

}
