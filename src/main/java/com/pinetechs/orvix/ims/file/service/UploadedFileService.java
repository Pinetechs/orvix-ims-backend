package com.pinetechs.orvix.ims.file.service;

import com.pinetechs.orvix.ims.config.Config;
import com.pinetechs.orvix.ims.config.Property;
import com.pinetechs.orvix.ims.config.WebMvcConfig;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.file.repository.UploadedFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UploadedFileService {

    private final UploadedFileRepository uploadedFileRepository;
    private final Config config;

    public UploadedFileService(UploadedFileRepository uploadedFileRepository, Config config) {
        this.uploadedFileRepository = uploadedFileRepository;
        this.config = config;
    }

    public UploadedFile upload(MultipartFile multipartFile, String folderName, boolean temp) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        validateFileSize(multipartFile);

        String safeFolder = normalizeFolder(folderName == null || folderName.trim().isEmpty()
                ? config.getProperty(Property.FILE_UPLOAD_DIR)
                : folderName);

        String originalFileName = StringUtils.cleanPath(multipartFile.getOriginalFilename() == null ? "file" : multipartFile.getOriginalFilename());
        String extension = extractExtension(originalFileName);
        String generatedName = UUID.randomUUID() + extension;

        File targetDirectory = new File(WebMvcConfig.getPublicDirectory(), safeFolder);
        if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
            throw new IOException("Could not create upload directory");
        }

        Path targetPath = new File(targetDirectory, generatedName).toPath();
        Files.copy(multipartFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setFileName(generatedName);
        uploadedFile.setOriginalFileName(originalFileName);
        uploadedFile.setFilePath(targetPath.toAbsolutePath().toString());
        uploadedFile.setPublicUrl("/public/" + safeFolder + generatedName);
        uploadedFile.setContentType(multipartFile.getContentType());
        uploadedFile.setExtension(extension.replace(".", ""));
        uploadedFile.setFileSize(multipartFile.getSize());
        uploadedFile.setUploadFolderName(safeFolder);
        uploadedFile.setTemp(temp);
        uploadedFile.setDeleted(false);

        return uploadedFileRepository.save(uploadedFile);
    }

    @Transactional(readOnly = true)
    public List<UploadedFile> getListById(List<Long> ids) {
        return uploadedFileRepository.findByIdInAndDeletedFalse(ids);
    }

    private void validateFileSize(MultipartFile file) {
        Integer maxFileSizeMb = config.getProperty(Property.MAX_FILE_SIZE_MB);
        long maxBytes = (maxFileSizeMb == null ? 50L : maxFileSizeMb.longValue()) * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size");
        }
    }

    private String normalizeFolder(String folder) {
        String value = folder.replace("\\", "/").trim();
        while (value.startsWith("/")) value = value.substring(1);
        if (!value.endsWith("/")) value = value + "/";
        if (value.contains("..")) {
            throw new IllegalArgumentException("Invalid folder path");
        }
        return value;
    }

    private String extractExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return fileName.substring(index).toLowerCase();
    }
}
