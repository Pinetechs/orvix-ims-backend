package com.pinetechs.orvix.ims.file.service;

import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.file.repository.UploadedFileRepository;
import com.pinetechs.orvix.ims.file.enums.UploadedFileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TemporaryFileCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TemporaryFileCleanupJob.class);
    private final UploadedFileRepository uploadedFileRepository;
    private final UploadedFileService uploadedFileService;

    public TemporaryFileCleanupJob(
            UploadedFileRepository uploadedFileRepository,
            UploadedFileService uploadedFileService
    ) {
        this.uploadedFileRepository = uploadedFileRepository;
        this.uploadedFileService = uploadedFileService;
    }

    @Scheduled(cron = "0 20 2 * * *")
    public void deleteExpiredTemporaryFiles() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        for (UploadedFile file : uploadedFileRepository
                .findByFileTypeAndTempTrueAndDeletedFalseAndCreatedAtBefore(UploadedFileType.SCAN_IMAGE, cutoff)) {
            try {
                uploadedFileService.deletePhysicalFile(file.getId());
            } catch (Exception ex) {
                log.warn("Could not delete temporary uploaded file. fileId={}", file.getId(), ex);
            }
        }
    }
}
