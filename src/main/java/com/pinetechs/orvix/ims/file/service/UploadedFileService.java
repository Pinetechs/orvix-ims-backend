package com.pinetechs.orvix.ims.file.service;

import com.pinetechs.orvix.ims.config.Config;
import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.config.Property;
import com.pinetechs.orvix.ims.config.WebMvcConfig;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.file.enums.UploadedFileType;
import com.pinetechs.orvix.ims.file.enums.UploadedFileVisibility;
import com.pinetechs.orvix.ims.file.repository.UploadedFileRepository;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UploadedFileService {

    private static final long MAX_SCAN_IMAGE_BYTES = 2L * 1024L * 1024L;
    private static final Set<String> SCAN_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

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
        UploadedFile uploadedFile = upload(multipartFile, folderName, temp, publicFile);
        uploadedFile.setFileType(UploadedFileType.IMPORT_EXCEL);
        return uploadedFileRepository.save(uploadedFile);
    }

    /**
     * Persists scan evidence in a separate transaction. If the scan transaction
     * fails, the file remains temporary and is removed later by the cleanup job.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UploadedFile uploadPrivateScanImage(
            MultipartFile multipartFile,
            String folderName,
            User uploadedBy
    ) throws IOException {
        validateScanImage(multipartFile);
        UploadedFile uploadedFile = upload(multipartFile, folderName, true, false);
        try {
            uploadedFile.setFileType(UploadedFileType.SCAN_IMAGE);
            uploadedFile.setVisibility(UploadedFileVisibility.PRIVATE);
            uploadedFile.setUploadedBy(uploadedBy);
            return uploadedFileRepository.save(uploadedFile);
        } catch (RuntimeException ex) {
            Files.deleteIfExists(Path.of(uploadedFile.getFilePath()));
            throw ex;
        }
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
        uploadedFile.setVisibility(publicFile
                ? UploadedFileVisibility.PUBLIC
                : UploadedFileVisibility.PRIVATE);
        uploadedFile.setFileType(UploadedFileType.GENERIC);
        uploadedFile.setChecksumSha256(sha256(targetPath));

        try {
            return uploadedFileRepository.save(uploadedFile);
        } catch (RuntimeException ex) {
            Files.deleteIfExists(targetPath);
            throw ex;
        }
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

    public UploadedFile markAsAttached(Long uploadedFileId) {
        if (uploadedFileId == null) {
            throw new IllegalArgumentException("Uploaded file id is required");
        }

        int updatedRows = uploadedFileRepository.markAsAttached(uploadedFileId);
        if (updatedRows == 0) {
            throw new IllegalArgumentException("Uploaded file not found");
        }

        return uploadedFileRepository.getReferenceById(uploadedFileId);
    }

    /**
     * Detaches an unused file and returns it to the temporary-file cleanup flow.
     * The database row remains available until the scheduled cleanup removes the
     * physical file, which also makes task deletion transaction-safe.
     */
    public void markForCleanup(Long uploadedFileId) {
        if (uploadedFileId == null) {
            return;
        }
        uploadedFileRepository.findById(uploadedFileId).ifPresent(file -> {
            if (!Boolean.TRUE.equals(file.getDeleted())) {
                file.setTemp(true);
                uploadedFileRepository.save(file);
            }
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

        if (!Boolean.TRUE.equals(file.getTemp()) && !Boolean.TRUE.equals(file.getDeleted())) {
            throw new IllegalStateException("Attached active file must be marked as deleted before physical deletion");
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

    private void validateScanImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Scan image is required");
        }
        if (file.getSize() > MAX_SCAN_IMAGE_BYTES) {
            throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "Scan image exceeds the 2 MB limit");
        }

        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName == null ? "" : originalName)
                .replace(".", "")
                .toLowerCase(Locale.ROOT);
        if (!SCAN_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPEG, PNG and WebP scan images are allowed");
        }

        byte[] header = new byte[12];
        int read;
        try (InputStream input = file.getInputStream()) {
            read = input.readNBytes(header, 0, header.length);
        }
        String detected = detectImageType(header, read);
        boolean extensionMatches = ("jpeg".equals(detected) && ("jpg".equals(extension) || "jpeg".equals(extension)))
                || detected.equals(extension);
        if (detected.isEmpty() || !extensionMatches) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Scan image content does not match its file extension");
        }
    }

    private String detectImageType(byte[] header, int length) {
        if (length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff) {
            return "jpeg";
        }
        if (length >= 8
                && (header[0] & 0xff) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                && (header[4] & 0xff) == 0x0d && (header[5] & 0xff) == 0x0a
                && (header[6] & 0xff) == 0x1a && (header[7] & 0xff) == 0x0a) {
            return "png";
        }
        if (length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "webp";
        }
        return "";
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest.digest()) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
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
