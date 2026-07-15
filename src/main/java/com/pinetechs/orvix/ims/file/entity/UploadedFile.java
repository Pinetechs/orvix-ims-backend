package com.pinetechs.orvix.ims.file.entity;

import com.pinetechs.orvix.ims.file.enums.UploadedFileType;
import com.pinetechs.orvix.ims.file.enums.UploadedFileVisibility;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "uploaded_files", indexes = {
        @Index(name = "idx_uploaded_files_folder", columnList = "upload_folder_name"),
        @Index(name = "idx_uploaded_files_temp", columnList = "is_temp"),
        @Index(name = "idx_uploaded_files_deleted", columnList = "is_deleted")
})
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", length = 500)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "public_url", nullable = false, length = 1000)
    private String publicUrl;

    @Column(name = "content_type", length = 200)
    private String contentType;

    @Column(name = "extension", length = 50)
    private String extension;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "upload_folder_name", length = 200)
    private String uploadFolderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private UploadedFileVisibility visibility = UploadedFileVisibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 30)
    private UploadedFileType fileType = UploadedFileType.GENERIC;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedBy;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "is_temp", nullable = false)
    private Boolean temp = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (deleted == null) deleted = false;
        if (temp == null) temp = true;
        if (visibility == null) visibility = UploadedFileVisibility.PRIVATE;
        if (fileType == null) fileType = UploadedFileType.GENERIC;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getUploadFolderName() { return uploadFolderName; }
    public void setUploadFolderName(String uploadFolderName) { this.uploadFolderName = uploadFolderName; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

    public Boolean getTemp() { return temp; }
    public void setTemp(Boolean temp) { this.temp = temp; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public UploadedFileVisibility getVisibility() { return visibility; }
    public void setVisibility(UploadedFileVisibility visibility) { this.visibility = visibility; }

    public UploadedFileType getFileType() { return fileType; }
    public void setFileType(UploadedFileType fileType) { this.fileType = fileType; }

    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }

    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
}
