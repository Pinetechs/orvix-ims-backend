package com.pinetechs.orvix.ims.app.service;

import com.pinetechs.orvix.ims.app.dto.AppHierarchyOptionResponse;
import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryFloor;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryFloorRepository;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryLocationAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryPlaceRepository;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryBranchAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryLocationRepository;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppHierarchyService {

    private final AppScanSupport scanSupport;
    private final AssetInventoryLocationAssignmentRepository assetAssignmentRepository;
    private final AssetInventoryFloorRepository floorRepository;
    private final AssetInventoryPlaceRepository placeRepository;
    private final SparePartInventoryBranchAssignmentRepository branchAssignmentRepository;
    private final SparePartInventoryLocationRepository spareLocationRepository;

    public AppHierarchyService(
            AppScanSupport scanSupport,
            AssetInventoryLocationAssignmentRepository assetAssignmentRepository,
            AssetInventoryFloorRepository floorRepository,
            AssetInventoryPlaceRepository placeRepository,
            SparePartInventoryBranchAssignmentRepository branchAssignmentRepository,
            SparePartInventoryLocationRepository spareLocationRepository
    ) {
        this.scanSupport = scanSupport;
        this.assetAssignmentRepository = assetAssignmentRepository;
        this.floorRepository = floorRepository;
        this.placeRepository = placeRepository;
        this.branchAssignmentRepository = branchAssignmentRepository;
        this.spareLocationRepository = spareLocationRepository;
    }

    @Transactional(readOnly = true)
    public List<AppHierarchyOptionResponse> assetFloors(Long taskId, Long locationId, User user) {
        scanSupport.requireAssignedScannableTask(taskId, user, InventoryDomain.ASSET);
        if (!assetAssignmentRepository.existsActiveByTaskIdAndUserIdAndLocationId(taskId, user.getId(), locationId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Asset location is not assigned to the current user");
        }
        return floorRepository.findByLocationIdOrderByFloorNameAsc(locationId).stream()
                .filter(floor -> taskId.equals(floor.getInventoryTask().getId()))
                .map(floor -> new AppHierarchyOptionResponse(floor.getId(), null, floor.getFloorName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppHierarchyOptionResponse> assetPlaces(Long taskId, Long floorId, User user) {
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
        return placeRepository.findByFloorIdOrderByPlaceNameAsc(floorId).stream()
                .filter(place -> taskId.equals(place.getInventoryTask().getId()))
                .map(place -> new AppHierarchyOptionResponse(place.getId(), null, place.getPlaceName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppHierarchyOptionResponse> sparePartLocations(Long taskId, Long branchId, User user) {
        scanSupport.requireAssignedScannableTask(taskId, user, InventoryDomain.SPARE_PART);
        if (!branchAssignmentRepository.existsActiveByTaskIdAndUserIdAndBranchId(taskId, user.getId(), branchId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Spare part branch is not assigned to the current user");
        }
        return spareLocationRepository.findByBranchIdOrderByLocationCodeAsc(branchId).stream()
                .filter(location -> taskId.equals(location.getInventoryTask().getId()))
                .map(location -> new AppHierarchyOptionResponse(
                        location.getId(), location.getLocationCode(), location.getLocationCode()))
                .toList();
    }
}
