package com.pinetechs.orvix.ims.jobs.dto;

import com.pinetechs.orvix.ims.jobs.enums.JobStatus;

public class BackgroundJobResponse {
    private Long jobId;
    private String jobName ;
    private Long relatedId;
    private int progress;
    private JobStatus status;
    private String errorMessage;
    private String message;
    private String result;

    public BackgroundJobResponse() {
    }

    public BackgroundJobResponse(Long jobId, String jobName, Long relatedId, int progress, JobStatus status, String errorMessage, String message, String result) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.relatedId = relatedId;
        this.progress = progress;
        this.status = status;
        this.errorMessage = errorMessage;
        this.message = message;
        this.result = result;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
