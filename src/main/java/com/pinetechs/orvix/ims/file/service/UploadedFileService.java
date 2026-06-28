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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    public UploadedFile uploadExcel(
            MultipartFile multipartFile,
            String folderName,
            boolean temp,
            boolean publicFile
    ) throws IOException {
        validateAllowedExtensions(multipartFile, Set.of("xls", "xlsx"));
        return upload(multipartFile, folderName, temp, publicFile);
    }

    public UploadedFile upload(
            MultipartFile multipartFile,
            String folderName,
            boolean temp,
            boolean publicFile
    ) throws IOException {

        String directoryPath = publicFile
                ? WebMvcConfig.getPublicDirectory()
                : WebMvcConfig.getPrivateDirectory();

        String publicUrl = publicFile ? "/public/" : "/private/";

        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        validateFileSize(multipartFile);

        String safeFolder = normalizeFolder(
                folderName == null || folderName.trim().isEmpty()
                        ? config.getProperty(Property.FILE_UPLOAD_DIR)
                        : folderName
        );

        String originalFileName = StringUtils.cleanPath(
                multipartFile.getOriginalFilename() == null
                        ? "file"
                        : multipartFile.getOriginalFilename()
        );

        String extension = extractExtension(originalFileName);
        String generatedName = UUID.randomUUID() + extension;

        File targetDirectory = new File(directoryPath, safeFolder);

        if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
            throw new IOException("Could not create upload directory");
        }

        Path targetPath = new File(targetDirectory, generatedName).toPath();

        Files.copy(
                multipartFile.getInputStream(),
                targetPath,
                StandardCopyOption.REPLACE_EXISTING
        );

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setFileName(generatedName);
        uploadedFile.setOriginalFileName(originalFileName);
        uploadedFile.setFilePath(targetPath.toAbsolutePath().toString());
        uploadedFile.setPublicUrl(publicUrl + safeFolder + generatedName);
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

    public void markAsDeleted(Long uploadedFileId) {
        if (uploadedFileId == null) {
            return;
        }

        uploadedFileRepository.findById(uploadedFileId).ifPresent(file -> {
            file.setDeleted(true);
            file.setDeletedAt(LocalDateTime.now());
            uploadedFileRepository.save(file);
        });
    }

    public boolean deletePhysicalFile(Long uploadedFileId) throws IOException {
        if (uploadedFileId == null) {
            return false;
        }

        UploadedFile file = uploadedFileRepository.findById(uploadedFileId).orElse(null);

        if (file == null) {
            return false;
        }

        boolean deletedFromDisk = false;

        if (file.getFilePath() != null && !file.getFilePath().isBlank()) {
            Path path = Path.of(file.getFilePath());
            deletedFromDisk = Files.deleteIfExists(path);
        }

        file.setDeleted(true);
        file.setDeletedAt(LocalDateTime.now());
        uploadedFileRepository.save(file);

        return deletedFromDisk;
    }

    private void validateFileSize(MultipartFile file) {
        Integer maxFileSizeMb = config.getProperty(Property.MAX_FILE_SIZE_MB);
        long maxBytes = (maxFileSizeMb == null ? 50L : maxFileSizeMb.longValue()) * 1024L * 1024L;

        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size");
        }
    }

    private void validateAllowedExtensions(MultipartFile file, Set<String> allowedExtensions) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }

        String extension = extractExtension(originalFileName)
                .replace(".", "")
                .toLowerCase(Locale.ROOT);

        if (extension.isBlank() || !allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Only Excel files are allowed: .xls, .xlsx");
        }
    }

    private String normalizeFolder(String folder) {
        String value = folder.replace("\\", "/").trim();

        while (value.startsWith("/")) {
            value = value.substring(1);
        }

        if (!value.endsWith("/")) {
            value = value + "/";
        }

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

        return fileName.substring(index).toLowerCase(Locale.ROOT);
    }
}
