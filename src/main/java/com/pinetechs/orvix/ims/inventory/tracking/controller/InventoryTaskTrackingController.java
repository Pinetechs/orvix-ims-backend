package com.pinetechs.orvix.ims.inventory.tracking.controller;

import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.tracking.dto.InventoryTaskActivityResponse;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingResultFilter;
import com.pinetechs.orvix.ims.inventory.tracking.service.InventoryTrackingService;
import com.pinetechs.orvix.ims.inventory.tracking.service.InventoryTaskTimelineService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/tracking/tasks")
public class InventoryTaskTrackingController {

    private static final int MAX_PAGE_SIZE = 200;

    private final InventoryTaskTimelineService timelineService;
    private final InventoryTrackingService trackingService;
    private final Helper helper;

    public InventoryTaskTrackingController(
            InventoryTaskTimelineService timelineService,
            InventoryTrackingService trackingService,
            Helper helper
    ) {
        this.timelineService = timelineService;
        this.trackingService = trackingService;
        this.helper = helper;
    }

    @GetMapping
    public Page<TrackingResponses.TaskListItem> getTasks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) InventoryDomain domain,
            @RequestParam(required = false) InventoryTaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        return trackingService.findTasks(
                search, companyId, domain, status, pageable, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/overview")
    public TrackingResponses.TaskOverview getOverview(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return trackingService.overview(taskId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/areas")
    public List<TrackingResponses.Area> getAreas(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return trackingService.areas(taskId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/team")
    public List<TrackingResponses.TeamMember> getTeam(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return trackingService.team(taskId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/attention")
    public TrackingResponses.Attention getAttention(@PathVariable Long taskId, Authentication authentication) {
        return trackingService.attention(taskId, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/results")
    public Page<TrackingResponses.ResultItem> getResults(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "ALL") TrackingResultFilter filter,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size));
        return trackingService.results(
                taskId, filter, search, pageable, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/scan-events")
    public Page<TrackingResponses.ScanEvent> getScanEvents(
            @PathVariable Long taskId,
            @RequestParam(required = false) InventoryScanEventType eventType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size));
        return trackingService.scanEvents(
                taskId, eventType, search, pageable, helper.currentUser(authentication));
    }

    @GetMapping("/{taskId}/timeline")
    public Page<InventoryTaskActivityResponse> getTimeline(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size));
        return timelineService.getTimeline(
                taskId,
                pageable,
                helper.currentUser(authentication)
        );
    }

    @GetMapping("/{taskId}/scan-events/{scanId}/image")
    public ResponseEntity<Resource> getScanImage(
            @PathVariable Long taskId,
            @PathVariable Long scanId,
            Authentication authentication
    ) {
        TrackingResponses.ImageFile image = trackingService.image(
                taskId, scanId, helper.currentUser(authentication));
        Path path = Path.of(image.filePath()).normalize();
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Scan image file is unavailable");
        }
        String fileName = image.originalFileName() == null || image.originalFileName().isBlank()
                ? image.storedFileName()
                : image.originalFileName();
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(fileName == null ? "scan-image" : fileName, StandardCharsets.UTF_8)
                .build();
        MediaType mediaType;
        try {
            mediaType = image.contentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(image.contentType());
        } catch (IllegalArgumentException ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(image.fileSize() == null ? path.toFile().length() : image.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(new FileSystemResource(path));
    }

    private int safePage(int page) {
        return Math.max(page, 0);
    }

    private int safeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
