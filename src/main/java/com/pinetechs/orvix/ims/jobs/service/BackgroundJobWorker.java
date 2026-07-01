package com.pinetechs.orvix.ims.jobs.service;

import com.pinetechs.orvix.ims.config.Config;
import com.pinetechs.orvix.ims.config.Property;
import com.pinetechs.orvix.ims.jobs.entity.BackgroundJob;
import com.pinetechs.orvix.ims.jobs.enums.JobStatus;
import com.pinetechs.orvix.ims.jobs.repository.BackgroundJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
public class BackgroundJobWorker {

    private static final Logger log = LoggerFactory.getLogger(BackgroundJobWorker.class);

    private final BackgroundJobRepository backgroundJobRepository;
    private final BackgroundJobClaimService backgroundJobClaimService;
    private final BackgroundJobDispatcher backgroundJobDispatcher;
    private final Config config;
    private final ExecutorService executor;

    public BackgroundJobWorker(
            BackgroundJobRepository backgroundJobRepository,
            BackgroundJobClaimService backgroundJobClaimService,
            BackgroundJobDispatcher backgroundJobDispatcher,
            Config config,
            ExecutorService executor
    ) {
        this.backgroundJobRepository = backgroundJobRepository;
        this.backgroundJobClaimService = backgroundJobClaimService;
        this.backgroundJobDispatcher = backgroundJobDispatcher;
        this.config = config;
        this.executor = executor;
    }

    @Scheduled(
            fixedDelayString = "${jobs.worker.poll-delay-ms:5000}",
            initialDelayString = "${jobs.worker.initial-delay-ms:1000}"
    )
    public void pollAndProcess() {

        boolean enabled = config.getProperty(Property.BACKGROUND_JOB_ENABLED);
        int batchSize = config.getProperty(Property.BACKGROUND_BATCH_SIZE);

        if (!enabled) {
            log.debug("Background job worker is disabled.");
            return;
        }

        long start = System.currentTimeMillis();

        try {
            LocalDateTime now = LocalDateTime.now();
            Pageable pageable = PageRequest.of(0, batchSize);

            List<Long> ids = backgroundJobRepository.findCandidateIds(JobStatus.PENDING, now, pageable);

            if (ids.isEmpty()) {
                log.debug("No pending background jobs found.");
                return;
            }

            log.info("Pending background jobs found. count={}, ids={}", ids.size(), ids);

            int claimed = backgroundJobClaimService.claimJobs(ids);

            log.info("Background jobs claimed. claimed={}, requested={}", claimed, ids.size());

            if (claimed == 0) {
                return;
            }

            List<BackgroundJob> jobs = backgroundJobRepository.findByIdInAndStatus(ids, JobStatus.RUNNING);

            for (BackgroundJob job : jobs) {
                Long jobId = job.getId();

                log.info(
                        "Submitting background job to executor. jobId={}, type={}, uploadedFileId={}",
                        jobId,
                        job.getJobType(),
                        job.getUploadedFileId()
                );

                executor.submit(() -> processJob(jobId));
            }

        } catch (Exception ex) {
            log.error("Error during background job polling cycle.", ex);

        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("Background job polling cycle finished in {} ms.", duration);
        }
    }

    private void processJob(Long jobId) {
        try {
            BackgroundJob job = backgroundJobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Background job not found: " + jobId));

            log.info("Background job execution started. jobId={}, type={}", job.getId(), job.getJobType());

            backgroundJobDispatcher.dispatch(job);

            log.info("Background job execution finished. jobId={}, type={}", job.getId(), job.getJobType());

        } catch (Exception ex) {
            log.error("Unhandled background job failure. jobId={}", jobId, ex);

            backgroundJobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(JobStatus.FAILED);
                job.setMessage("Job failed");
                job.setErrorMessage(ex.getMessage());
                job.setFinishedAt(LocalDateTime.now());
                backgroundJobRepository.save(job);
            });
        }
    }
}
