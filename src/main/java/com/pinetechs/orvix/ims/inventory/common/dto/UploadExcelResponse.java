package com.pinetechs.orvix.ims.inventory.common.dto;

public class UploadExcelResponse {

    private Long jobId;
    private Long taskId;
    private Long uploadedFileId;
    private String filePath;
    private String fileName;
    private String originalFileName;
    private String status;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getUploadedFileId() { return uploadedFileId; }
    public void setUploadedFileId(Long uploadedFileId) { this.uploadedFileId = uploadedFileId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
