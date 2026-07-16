package com.pinetechs.orvix.ims.app.service;

import com.pinetechs.orvix.ims.app.dto.AppHierarchyOptionResponse;
import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryFloor;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryFloorRepository;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryLocationAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryPlaceRepository;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryScanRepository;
import com.pinetechs.orvix.ims.inventory.common.dto.HierarchyScanProgress;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryHierarchyProgressStatus;
import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryLocation;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartLocationProgressMode;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryBranchAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryLocationRepository;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryScanRepository;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AppHierarchyService {

    private final AppScanSupport scanSupport;
    private final AssetInventoryLocationAssignmentRepository assetAssignmentRepository;
    private final AssetInventoryFloorRepository floorRepository;
    private final AssetInventoryPlaceRepository placeRepository;
    private final AssetInventoryScanRepository assetScanRepository;
    private final SparePartInventoryBranchAssignmentRepository branchAssignmentRepository;
    private final SparePartInventoryLocationRepository spareLocationRepository;
    private final SparePartInventoryScanRepository spareScanRepository;

    public AppHierarchyService(
            AppScanSupport scanSupport,
            AssetInventoryLocationAssignmentRepository assetAssignmentRepository,
            AssetInventoryFloorRepository floorRepository,
            AssetInventoryPlaceRepository placeRepository,
            AssetInventoryScanRepository assetScanRepository,
            SparePartInventoryBranchAssignmentRepository branchAssignmentRepository,
            SparePartInventoryLocationRepository spareLocationRepository,
            SparePartInventoryScanRepository spareScanRepository
    ) {
        this.scanSupport = scanSupport;
        this.assetAssignmentRepository = assetAssignmentRepository;
        this.floorRepository = floorRepository;
        this.placeRepository = placeRepository;
        this.assetScanRepository = assetScanRepository;
        this.branchAssignmentRepository = branchAssignmentRepository;
        this.spareLocationRepository = spareLocationRepository;
        this.spareScanRepository = spareScanRepository;
    }

    @Transactional(readOnly = true)
    public List<AppHierarchyOptionResponse> assetFloors(
            Long taskId,
            Long locationId,
            String search,
            User user
    ) {
        scanSupport.requireAssignedScannableTask(taskId, user, InventoryDomain.ASSET);
        if (!assetAssignmentRepository.existsActiveByTaskIdAndUserIdAndLocationId(taskId, user.getId(), locationId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Asset location is not assigned to the current user");
        }
        Map<Long, HierarchyScanProgress> progressByFloor = progressById(
                assetScanRepository.summarizeFloorProgress(taskId, locationId));
        return floorRepository.searchForApp(taskId, locationId, normalizeSearch(search)).stream()
                .map(floor -> basicOption(
                        floor.getId(), null, floor.getFloorName(), progressByFloor.get(floor.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppHierarchyOptionResponse> assetPlaces(
            Long taskId,
            Long floorId,
            String search,
            User user
    ) {
        scanSupport.requireAssignedScannableTask(taskId, user, InventoryDomain.ASSET);
        AssetInventoryFloor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Asset floor not found"));
        if (!taskId.equals(floor.getInventoryTask().getId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Asset floor not found");
        }
        if (!assetAssignmentRepository.existsActiveByTaskIdAndUserIdAndLocationId(
                taskId, user.getId(), floor.getLocation().getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Asset location is not assigned to the current user");
        }
        Map<Long, HierarchyScanProgress> progressByPlace = progressById(
                assetScanRepository.summarizePlaceProgress(taskId, floorId));
        return placeRepository.searchForApp(taskId, floorId, normalizeSearch(search)).stream()
                .map(place -> basicOption(
                        place.getId(), null, place.getPlaceName(), progressByPlace.get(place.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppHierarchyOptionResponse> sparePartLocations(
            Long taskId,
            Long branchId,
            String search,
            User user
    ) {
        InventoryTask task = scanSupport.requireAssignedScannableTask(
                taskId, user, InventoryDomain.SPARE_PART);
        if (!branchAssignmentRepository.existsActiveByTaskIdAndUserIdAndBranchId(taskId, user.getId(), branchId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Spare part branch is not assigned to the current user");
        }
        String normalizedSearch = normalizeSearch(search);
        Map<Long, HierarchyScanProgress> progressByLocation = progressById(
                spareScanRepository.summarizeLocationProgress(taskId, branchId));
        return spareLocationRepository.searchForApp(taskId, branchId, normalizedSearch).stream()
                .map(location -> spareLocationOption(
                        task, location, progressByLocation.get(location.getId())))
                .toList();
    }

    @Transactional
    public AppHierarchyOptionResponse completeSparePartLocation(
            Long taskId,
            Long branchId,
            Long locationId,
            User user
    ) {
        InventoryTask task = scanSupport.requireAssignedScannableTask(
                taskId, user, InventoryDomain.SPARE_PART);
        if (task.getSparePartLocationProgressMode() != SparePartLocationProgressMode.DETAILED) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Location completion is not enabled for this task");
        }
        if (!branchAssignmentRepository.existsActiveByTaskIdAndUserIdAndBranchId(
                taskId, user.getId(), branchId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Spare part branch is not assigned to the current user");
        }

        SparePartInventoryLocation location = spareLocationRepository
                .findForUpdate(taskId, branchId, locationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "Spare part location not found"));
        long scanCount = spareScanRepository.countLocationScans(taskId, locationId);
        if (scanCount == 0L) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "A location cannot be completed before its first scan");
        }
        long openReviews = spareScanRepository.countOpenLocationReviews(taskId, locationId);
        if (openReviews > 0L) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Resolve location scan reviews before completing this location");
        }

        if (!location.isCompleted()) {
            location.setCompleted(true);
            location.setCompletedAt(LocalDateTime.now());
            location.setCompletedBy(user);
            spareLocationRepository.saveAndFlush(location);
        }

        HierarchyScanProgress progress = spareScanRepository
                .summarizeLocationProgress(taskId, branchId).stream()
                .filter(value -> locationId.equals(value.getHierarchyId()))
                .findFirst()
                .orElse(new HierarchyScanProgress(locationId, scanCount, null, 0L));
        return spareLocationOption(task, location, progress);
    }

    private AppHierarchyOptionResponse basicOption(
            Long id,
            String code,
            String name,
            HierarchyScanProgress progress
    ) {
        long scanCount = progress == null ? 0L : progress.getScanCount();
        return new AppHierarchyOptionResponse(
                id,
                code,
                name,
                scanCount,
                progress == null ? null : progress.getLastScanAt(),
                scanCount == 0L
                        ? InventoryHierarchyProgressStatus.NOT_STARTED
                        : InventoryHierarchyProgressStatus.IN_PROGRESS,
                false,
                false,
                null
        );
    }

    private AppHierarchyOptionResponse spareLocationOption(
            InventoryTask task,
            SparePartInventoryLocation location,
            HierarchyScanProgress progress
    ) {
        long scanCount = progress == null ? 0L : progress.getScanCount();
        long reviewCount = progress == null ? 0L : progress.getReviewRequiredCount();
        boolean detailed = task.getSparePartLocationProgressMode()
                == SparePartLocationProgressMode.DETAILED;

        InventoryHierarchyProgressStatus status;
        if (detailed && reviewCount > 0L) {
            status = InventoryHierarchyProgressStatus.REVIEW_REQUIRED;
        } else if (detailed && location.isCompleted()) {
            status = InventoryHierarchyProgressStatus.COMPLETED;
        } else if (scanCount > 0L) {
            status = InventoryHierarchyProgressStatus.IN_PROGRESS;
        } else {
            status = InventoryHierarchyProgressStatus.NOT_STARTED;
        }

        return new AppHierarchyOptionResponse(
                location.getId(),
                location.getLocationCode(),
                location.getLocationCode(),
                scanCount,
                progress == null ? null : progress.getLastScanAt(),
                status,
                detailed,
                detailed && scanCount > 0L && reviewCount == 0L && !location.isCompleted(),
                detailed && location.isCompleted() ? location.getCompletedAt() : null
        );
    }

    private Map<Long, HierarchyScanProgress> progressById(List<HierarchyScanProgress> progress) {
        return progress.stream().collect(Collectors.toMap(
                HierarchyScanProgress::getHierarchyId,
                Function.identity()
        ));
    }

    private String normalizeSearch(String search) {
        if (search == null) return null;
        String normalized = search.trim();
        if (normalized.isEmpty()) return null;
        if (normalized.length() > 255) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Search text is too long");
        }
        return normalized;
    }
}
