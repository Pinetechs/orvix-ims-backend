package com.pinetechs.orvix.ims.file.dto;

import com.pinetechs.orvix.ims.file.entity.UploadedFile;

public class UploadedFileResponse {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String publicUrl;
    private String contentType;
    private Long fileSize;

    public static UploadedFileResponse from(UploadedFile file) {
        UploadedFileResponse response = new UploadedFileResponse();
        response.id = file.getId();
        response.fileName = file.getFileName();
        response.originalFileName = file.getOriginalFileName();
        response.publicUrl = file.getPublicUrl();
        response.contentType = file.getContentType();
        response.fileSize = file.getFileSize();
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
}
