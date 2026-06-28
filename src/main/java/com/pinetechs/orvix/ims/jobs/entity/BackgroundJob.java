package com.pinetechs.orvix.ims.jobs.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pinetechs.orvix.ims.jobs.enums.JobPriority;
import com.pinetechs.orvix.ims.jobs.enums.JobStatus;
import com.pinetechs.orvix.ims.jobs.enums.JobType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "background_jobs",
        indexes = {
                @Index(name = "idx_background_jobs_status", columnList = "status"),
                @Index(name = "idx_background_jobs_job_type", columnList = "job_type"),
                @Index(name = "idx_background_jobs_scheduled_time", columnList = "scheduled_time"),
                @Index(name = "idx_background_jobs_uploaded_file", columnList = "uploaded_file_id")
        }
)
public class BackgroundJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 150)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private JobStatus status = JobStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private JobPriority priority = JobPriority.NORMAL;

    @Column(name = "progress", nullable = false)
    private Integer progress = 0;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "related_file", length = 1000)
    private String relatedFile;

    @Column(name = "uploaded_file_id")
    private Long uploadedFileId;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retry", nullable = false)
    private Integer maxRetry = 3;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BackgroundJob() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public JobPriority getPriority() { return priority; }
    public void setPriority(JobPriority priority) { this.priority = priority; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public Long getRelatedId() { return relatedId; }
    public void setRelatedId(Long relatedId) { this.relatedId = relatedId; }

    public String getRelatedFile() { return relatedFile; }
    public void setRelatedFile(String relatedFile) { this.relatedFile = relatedFile; }

    public Long getUploadedFileId() { return uploadedFileId; }
    public void setUploadedFileId(Long uploadedFileId) { this.uploadedFileId = uploadedFileId; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getMaxRetry() { return maxRetry; }
    public void setMaxRetry(Integer maxRetry) { this.maxRetry = maxRetry; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
