package com.pinetechs.orvix.ims.app.service;

import com.pinetechs.orvix.ims.app.dto.AppScanCorrectionRequest;
import com.pinetechs.orvix.ims.app.dto.AppScanRequest;
import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanImageSource;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Component
public class AppScanSupport {

    private final InventoryTaskAssignmentRepository assignmentRepository;
    private final AccessPolicyService accessPolicyService;

    public AppScanSupport(
            InventoryTaskAssignmentRepository assignmentRepository,
            AccessPolicyService accessPolicyService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.accessPolicyService = accessPolicyService;
    }

    public InventoryTask requireAssignedScannableTask(Long taskId, User user, InventoryDomain expectedDomain) {
        InventoryTask task = requireAssignedScannableTask(taskId, user);
        if (task.getInventoryDomain() != expectedDomain) {
            throw new BusinessException(HttpStatus.CONFLICT, "Inventory task domain does not match the scan request");
        }
        return task;
    }

    public InventoryTask requireAssignedScannableTask(Long taskId, User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        accessPolicyService.assertCanCreateAppScan(user);
        InventoryTaskAssignment assignment = assignmentRepository
                .findActiveByTaskIdAndUserIdWithTaskAndCompany(taskId, user.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Assigned inventory task not found"));
        InventoryTask task = assignment.getInventoryTask();
        if (task.getStatus() != InventoryTaskStatus.READY_TO_START
                && task.getStatus() != InventoryTaskStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT, "Inventory task is not open for scanning");
        }
        return task;
    }

    public void requireCorrectionPermission(User user) {
        accessPolicyService.assertCanCorrectAppScan(user);
    }

    public void requireQuantityPermission(User user) {
        accessPolicyService.assertCanEnterAppQuantity(user);
    }

    public void validateClientScanId(String clientScanId) {
        if (clientScanId == null || clientScanId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "clientScanId is required");
        }
        try {
            UUID.fromString(clientScanId.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "clientScanId must be a UUID");
        }
    }

    public String requireCode(String value) {
        return requireCode(value, 150);
    }

    public String requireCode(String value, int maxLength) {
        String normalized = normalizeCode(value);
        if (normalized == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Scanned code is required");
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Scanned code is too long");
        }
        return normalized;
    }

    public String normalizeCode(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    public void requireImageWhenConfigured(InventoryTask task, MultipartFile image) {
        if (task.isScanImageRequired() && (image == null || image.isEmpty())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Scan image is required for this task");
        }
    }

    public String scanFingerprint(AppScanRequest request, MultipartFile image) throws IOException {
        String canonical = String.join("|",
                value(request.getClientScanId()), value(normalizeCode(request.getCode())),
                value(request.getBranchId()), value(request.getLocationId()), value(request.getFloorId()),
                value(request.getPlaceId()), value(request.getCountedQty()), value(request.getDeviceScannedAt()),
                value(trim(request.getDeviceId(), 150)), value(trim(request.getSymbology(), 80)),
                value(request.getImageSource()), value(trim(request.getNotes(), 1000)), fileHash(image));
        return sha256(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public String correctionFingerprint(Long correctedScanId, AppScanCorrectionRequest request, MultipartFile image) throws IOException {
        String canonical = String.join("|", "CORRECTION", value(correctedScanId),
                value(request.getClientScanId()), value(trim(request.getReason(), 1000)),
                value(request.getBranchId()), value(request.getLocationId()), value(request.getFloorId()),
                value(request.getPlaceId()), value(request.getCountedQty()), value(request.getDeviceScannedAt()),
                value(trim(request.getDeviceId(), 150)), value(trim(request.getSymbology(), 80)),
                value(request.getImageSource()), fileHash(image));
        return sha256(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public InventoryScanImageSource imageSource(String value) {
        if (value == null || value.isBlank()) return InventoryScanImageSource.UNKNOWN;
        try {
            return InventoryScanImageSource.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Unsupported imageSource");
        }
    }

    public void startTaskIfNeeded(InventoryTask task) {
        if (task.getStatus() == InventoryTaskStatus.READY_TO_START) {
            task.setStatus(InventoryTaskStatus.IN_PROGRESS);
            if (task.getStartDate() == null) task.setStartDate(LocalDate.now());
            if (task.getStartedAt() == null) task.setStartedAt(LocalDateTime.now());
        }
    }

    public int safe(Integer value) { return value == null ? 0 : value; }

    public String trim(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > maxLength) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Text value exceeds maximum length " + maxLength);
        }
        return trimmed;
    }

    private String fileHash(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return "NO_IMAGE";
        try (InputStream input = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String sha256(byte[] value) {
        try {
            return toHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }

    private String value(Object value) { return value == null ? "" : String.valueOf(value); }
}
