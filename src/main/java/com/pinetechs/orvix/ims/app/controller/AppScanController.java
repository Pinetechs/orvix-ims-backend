package com.pinetechs.orvix.ims.app.controller;

import com.pinetechs.orvix.ims.app.dto.AppScanCorrectionRequest;
import com.pinetechs.orvix.ims.app.dto.AppScanRequest;
import com.pinetechs.orvix.ims.app.dto.AppScanResponse;
import com.pinetechs.orvix.ims.app.service.AppScanService;
import com.pinetechs.orvix.ims.app.service.AppScanImageService;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.common.service.Helper;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/app/v1/tasks")
public class AppScanController {

    private final AppScanService appScanService;
    private final Helper helper;
    private final AppScanImageService appScanImageService;

    public AppScanController(AppScanService appScanService, AppScanImageService appScanImageService, Helper helper) {
        this.appScanService = appScanService;
        this.helper = helper;
        this.appScanImageService = appScanImageService;
    }

    @GetMapping("/{taskId}/scans/{scanId}/image")
    public ResponseEntity<Resource> image(@PathVariable Long taskId, @PathVariable Long scanId, Authentication authentication) {
        UploadedFile file = appScanImageService.getAuthorizedImage(taskId, scanId, helper.currentUser(authentication));


        Resource resource = new FileSystemResource(file.getFilePath());
        if (!resource.exists() || !resource.isReadable()) {
            throw new com.pinetechs.orvix.ims.common.exception.BusinessException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Scan image content not found");
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
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"scan-image\"")
                .body(resource);
    }

    @PostMapping(value = "/{taskId}/scans", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AppScanResponse scan(@PathVariable Long taskId, @RequestPart("request") AppScanRequest request, @RequestPart(name = "image", required = false) MultipartFile image, Authentication authentication
    ) throws IOException {
        return appScanService.scan(taskId, request, image, helper.currentUser(authentication));
    }

    @PostMapping(value = "/{taskId}/scans/{currentScanId}/corrections",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AppScanResponse correct(
            @PathVariable Long taskId,
            @PathVariable Long currentScanId,
            @RequestPart("request") AppScanCorrectionRequest request,
            @RequestPart(name = "image", required = false) MultipartFile image,
            Authentication authentication
    ) throws IOException {
        return appScanService.correct(taskId, currentScanId, request, image, helper.currentUser(authentication));
    }
}
