package com.pinetechs.orvix.ims.inventory.review.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.file.service.UploadedFileService;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanImageSource;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.review.domain.ReviewDomainHandlerRegistry;
import com.pinetechs.orvix.ims.inventory.review.dto.RecheckRequestResponse;
import com.pinetechs.orvix.ims.inventory.review.dto.SubmitRecheckItemRequest;
import com.pinetechs.orvix.ims.inventory.review.entity.*;
import com.pinetechs.orvix.ims.inventory.review.enums.*;
import com.pinetechs.orvix.ims.inventory.review.repository.*;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.enums.InventoryTaskActivityType;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskActivityService;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class RecheckSubmissionService {

    private final InventoryRecheckItemRepository itemRepository;
    private final InventoryRecheckRequestRepository requestRepository;
    private final UploadedFileService uploadedFileService;
    private final ReviewDomainHandlerRegistry handlerRegistry;
    private final ReviewResponseMapper mapper;
    private final AccessPolicyService accessPolicyService;
    private final InventoryTaskActivityService taskActivityService;

    public RecheckSubmissionService(
            InventoryRecheckItemRepository itemRepository,
            InventoryRecheckRequestRepository requestRepository,
            UploadedFileService uploadedFileService,
            ReviewDomainHandlerRegistry handlerRegistry,
            ReviewResponseMapper mapper,
            AccessPolicyService accessPolicyService,
            InventoryTaskActivityService taskActivityService
    ) {
        this.itemRepository = itemRepository;
        this.requestRepository = requestRepository;
        this.uploadedFileService = uploadedFileService;
        this.handlerRegistry = handlerRegistry;
        this.mapper = mapper;
        this.accessPolicyService = accessPolicyService;
        this.taskActivityService = taskActivityService;
    }

    @Transactional
    public RecheckRequestResponse submit(
            Long requestId,
            Long itemId,
            SubmitRecheckItemRequest input,
            MultipartFile image,
            User user
    ) throws IOException {
        accessPolicyService.assertCanUseApp(user);
        if (input == null) badRequest("Recheck submission is required");

        InventoryRecheckItem item = itemRepository
                .findForUpdate(requestId, itemId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Recheck item not found"));
        InventoryRecheckRequest request = item.getRecheckRequest();
        InventoryTask task = request.getInventoryTask();
        assertAssignedTo(request, user);
        requireUnderReview(task);
        validateClientSubmissionId(input.getClientSubmissionId());

        String normalizedClientId = input.getClientSubmissionId()
                .trim().toLowerCase(Locale.ROOT);
        String payloadHash = submissionFingerprint(input, image);
        if (item.getStatus() == RecheckItemStatus.SUBMITTED) {
            if (normalizedClientId.equals(item.getClientSubmissionId())
                    && payloadHash.equals(item.getPayloadHash())) {
                return mapper.recheckRequest(request);
            }
            throw new BusinessException(HttpStatus.CONFLICT,
                    "This recheck item was already submitted");
        }
        if (item.getStatus() != RecheckItemStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Recheck item is not open for submission");
        }
        if (request.isImageRequired() && (image == null || image.isEmpty())) {
            badRequest("Evidence image is required for this recheck request");
        }

        copySubmission(item, input, normalizedClientId, payloadHash);
        validateUnableResult(item);
        handlerRegistry.get(task.getInventoryDomain())
                .validateSubmission(task, item);

        UploadedFile evidence = uploadEvidence(task, request, image, user);
        item.setEvidenceImage(evidence);
        LocalDateTime now = LocalDateTime.now();
        item.submit(now);
        item.getIssues().stream()
                .filter(issue -> issue.getStatus().isOpen())
                .forEach(InventoryReviewIssue::markRecheckSubmitted);
        itemRepository.save(item);
        if (evidence != null) {
            uploadedFileService.markAsAttached(evidence.getId());
        }

        if (request.getStatus() == RecheckRequestStatus.PENDING) {
            request.start(now);
        }
        boolean allSubmitted = request.getItems().stream()
                .allMatch(value -> value.getStatus() == RecheckItemStatus.SUBMITTED);
        if (allSubmitted) {
            request.markSubmitted(now);
            taskActivityService.record(
                    task,
                    InventoryTaskActivityType.RECHECK_SUBMITTED,
                    task.getStatus(),
                    task.getStatus(),
                    user,
                    null,
                    "Recheck " + request.getRequestNumber() + " submitted"
            );
        }
        requestRepository.save(request);
        return mapper.recheckRequest(request);
    }

    private void copySubmission(
            InventoryRecheckItem item,
            SubmitRecheckItemRequest input,
            String clientId,
            String payloadHash
    ) {
        if (input.getResult() == null) badRequest("Recheck result is required");
        item.setClientSubmissionId(clientId);
        item.setPayloadHash(payloadHash);
        item.setResult(input.getResult());
        item.setResolvedItemId(input.getResolvedItemId());
        item.setScannedCode(normalizeCode(input.getScannedCode()));
        item.setBranchId(input.getBranchId());
        item.setLocationId(input.getLocationId());
        item.setFloorId(input.getFloorId());
        item.setPlaceId(input.getPlaceId());
        item.setCountedQuantity(input.getCountedQuantity());
        item.setReasonCode(input.getReasonCode());
        item.setNote(trim(input.getNote(), 1000));
        item.setDeviceScannedAt(input.getDeviceScannedAt());
        item.setDeviceId(trim(input.getDeviceId(), 150));
        item.setSymbology(trim(input.getSymbology(), 80));
        item.setImageSource(parseImageSource(input.getImageSource()));
    }

    private void validateUnableResult(InventoryRecheckItem item) {
        if (item.getResult() != RecheckResult.NOT_FOUND
                && item.getResult() != RecheckResult.UNABLE_TO_VERIFY) {
            return;
        }
        if (item.getReasonCode() == null) {
            badRequest("Reason code is required when the item was not verified");
        }
        if (item.getNote() == null) {
            badRequest("A note is required when the item was not verified");
        }
    }

    private UploadedFile uploadEvidence(
            InventoryTask task,
            InventoryRecheckRequest request,
            MultipartFile image,
            User user
    ) throws IOException {
        if (image == null || image.isEmpty()) return null;
        String folder = "inventory-tasks/" + task.getId()
                + "/rechecks/" + request.getId();
        return uploadedFileService.uploadPrivateScanImage(image, folder, user);
    }

    private String submissionFingerprint(
            SubmitRecheckItemRequest input,
            MultipartFile image
    ) throws IOException {
        String canonical = String.join("|",
                value(input.getClientSubmissionId()),
                value(input.getResult()),
                value(input.getResolvedItemId()),
                value(normalizeCode(input.getScannedCode())),
                value(input.getBranchId()),
                value(input.getLocationId()),
                value(input.getFloorId()),
                value(input.getPlaceId()),
                value(input.getCountedQuantity()),
                value(input.getReasonCode()),
                value(trim(input.getNote(), 1000)),
                value(input.getDeviceScannedAt()),
                value(trim(input.getDeviceId(), 150)),
                value(trim(input.getSymbology(), 80)),
                value(input.getImageSource()),
                fileHash(image)
        );
        return sha256(canonical.getBytes(StandardCharsets.UTF_8));
    }

    private String fileHash(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return "NO_IMAGE";
        try (InputStream input = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
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
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private void validateClientSubmissionId(String value) {
        if (value == null || value.isBlank()) {
            badRequest("clientSubmissionId is required");
        }
        try {
            UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            badRequest("clientSubmissionId must be a UUID");
        }
    }

    private InventoryScanImageSource parseImageSource(String value) {
        if (value == null || value.isBlank()) return InventoryScanImageSource.UNKNOWN;
        try {
            return InventoryScanImageSource.valueOf(
                    value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            badRequest("Unsupported imageSource");
            return InventoryScanImageSource.UNKNOWN;
        }
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 180) badRequest("Scanned code is too long");
        return normalized;
    }

    private void assertAssignedTo(InventoryRecheckRequest request, User user) {
        if (user == null || user.getId() == null
                || request.getAssignedTo() == null
                || !user.getId().equals(request.getAssignedTo().getId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "Assigned recheck request not found");
        }
    }

    private void requireUnderReview(InventoryTask task) {
        if (task.getStatus() != InventoryTaskStatus.UNDER_REVIEW) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Recheck is available only while the task is UNDER_REVIEW");
        }
    }

    private String trim(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > max) {
            badRequest("Text value exceeds maximum length " + max);
        }
        return normalized;
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void badRequest(String message) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, message);
    }
}
