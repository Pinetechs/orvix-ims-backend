package com.pinetechs.orvix.ims.app.controller;

import com.pinetechs.orvix.ims.app.dto.AppHierarchyOptionResponse;
import com.pinetechs.orvix.ims.app.service.AppRecheckLocationService;
import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.inventory.review.dto.*;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckRequestStatus;
import com.pinetechs.orvix.ims.inventory.review.service.RecheckSubmissionService;
import com.pinetechs.orvix.ims.inventory.review.service.RecheckWorkflowService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/app/v1/rechecks")
public class AppRecheckController {

    private final RecheckWorkflowService recheckWorkflowService;
    private final RecheckSubmissionService recheckSubmissionService;
    private final AppRecheckLocationService recheckLocationService;
    private final Helper helper;

    public AppRecheckController(
            RecheckWorkflowService recheckWorkflowService,
            RecheckSubmissionService recheckSubmissionService,
            AppRecheckLocationService recheckLocationService,
            Helper helper
    ) {
        this.recheckWorkflowService = recheckWorkflowService;
        this.recheckSubmissionService = recheckSubmissionService;
        this.recheckLocationService = recheckLocationService;
        this.helper = helper;
    }

    @GetMapping
    public Page<RecheckRequestResponse> requests(
            @RequestParam(required = false) RecheckRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return recheckWorkflowService.appRequests(
                status,
                PageRequest.of(page, size, Sort.by("createdAt").descending()),
                helper.currentUser(authentication)
        );
    }

    @GetMapping("/{requestId}")
    public RecheckRequestResponse request(
            @PathVariable Long requestId,
            Authentication authentication
    ) {
        return recheckWorkflowService.appRequest(
                requestId, helper.currentUser(authentication));
    }

    @PostMapping("/{requestId}/start")
    public RecheckRequestResponse start(
            @PathVariable Long requestId,
            Authentication authentication
    ) {
        return recheckWorkflowService.start(
                requestId, helper.currentUser(authentication));
    }

    @GetMapping("/{requestId}/items/{itemId}/floors")
    public List<AppHierarchyOptionResponse> assetFloors(
            @PathVariable Long requestId,
            @PathVariable Long itemId,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        return recheckLocationService.assetFloors(
                requestId,
                itemId,
                search,
                helper.currentUser(authentication)
        );
    }

    @GetMapping("/{requestId}/items/{itemId}/floors/{floorId}/places")
    public List<AppHierarchyOptionResponse> assetPlaces(
            @PathVariable Long requestId,
            @PathVariable Long itemId,
            @PathVariable Long floorId,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        return recheckLocationService.assetPlaces(
                requestId,
                itemId,
                floorId,
                search,
                helper.currentUser(authentication)
        );
    }

    @GetMapping("/{requestId}/items/{itemId}/locations")
    public List<AppHierarchyOptionResponse> sparePartLocations(
            @PathVariable Long requestId,
            @PathVariable Long itemId,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        return recheckLocationService.sparePartLocations(
                requestId,
                itemId,
                search,
                helper.currentUser(authentication)
        );
    }

    @PostMapping(
            value = "/{requestId}/items/{itemId}/submit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public RecheckRequestResponse submit(
            @PathVariable Long requestId,
            @PathVariable Long itemId,
            @RequestPart("request") SubmitRecheckItemRequest request,
            @RequestPart(name = "image", required = false) MultipartFile image,
            Authentication authentication
    ) throws IOException {
        return recheckSubmissionService.submit(
                requestId,
                itemId,
                request,
                image,
                helper.currentUser(authentication)
        );
    }

    @GetMapping("/{requestId}/items/{itemId}/evidence")
    public ResponseEntity<Resource> evidence(
            @PathVariable Long requestId,
            @PathVariable Long itemId,
            Authentication authentication
    ) {
        UploadedFile file = recheckWorkflowService.appEvidence(
                requestId, itemId, helper.currentUser(authentication));
        Resource resource = new FileSystemResource(file.getFilePath());
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Recheck evidence content not found");
        }
        MediaType contentType;
        try {
            contentType = file.getContentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(file.getContentType());
        } catch (IllegalArgumentException ex) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"recheck-evidence\"")
                .body(resource);
    }
}
