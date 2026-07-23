package com.pinetechs.orvix.ims.app.service;

import com.pinetechs.orvix.ims.app.dto.AppHierarchyOptionResponse;
import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryFloor;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryFloorRepository;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryPlaceRepository;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.review.entity.InventoryRecheckItem;
import com.pinetechs.orvix.ims.inventory.review.entity.InventoryRecheckRequest;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckItemStatus;
import com.pinetechs.orvix.ims.inventory.review.repository.InventoryRecheckItemRepository;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryLocationRepository;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Returns only location choices inside the assigned recheck.
 *
 * <p>This service deliberately does not reuse the regular scanning hierarchy.
 * That hierarchy requires an original task assignment and an IN_PROGRESS task,
 * while rechecks run during UNDER_REVIEW and may be assigned to another
 * inventory employee.</p>
 */
@Service
public class AppRecheckLocationService {

    private final InventoryRecheckItemRepository recheckItemRepository;
    private final AssetInventoryFloorRepository floorRepository;
    private final AssetInventoryPlaceRepository placeRepository;
    private final SparePartInventoryLocationRepository spareLocationRepository;
    private final AccessPolicyService accessPolicyService;

    public AppRecheckLocationService(
            InventoryRecheckItemRepository recheckItemRepository,
            AssetInventoryFloorRepository floorRepository,
            AssetInventoryPlaceRepository placeRepository,
            SparePartInventoryLocationRepository spareLocationRepository,
            AccessPolicyService accessPolicyService
    ) {
        this.recheckItemRepository = recheckItemRepository;
        this.floorRepository = floorRepository;
        this.placeRepository = placeRepository;
        this.spareLocationRepository = spareLocationRepository;
        this.accessPolicyService = accessPolicyService;
    }

    @Transactional(readOnly = true)
    public List<AppHierarchyOptionResponse> assetFloors(
            Long requestId,
            Long itemId,
            String search,
            User user
    ) {
        InventoryRecheckItem item = requireOpenAssignedItem(
                requestId, itemId, InventoryDomain.ASSET, user);
        Long locationId = requireId(
                item.getLocationId(), "Expected asset location is unavailable");
        Long taskId = item.getRecheckRequest().getInventoryTask().getId();

        return floorRepository.searchForApp(taskId, locationId, normalizeSearch(search))
                .stream()
                .map(floor -> new AppHierarchyOptionResponse(
                        floor.getId(), null, floor.getFloorName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppHierarchyOptionResponse> assetPlaces(
            Long requestId,
            Long itemId,
            Long floorId,
            String search,
            User user
    ) {
        InventoryRecheckItem item = requireOpenAssignedItem(
                requestId, itemId, InventoryDomain.ASSET, user);
        Long locationId = requireId(
                item.getLocationId(), "Expected asset location is unavailable");
        Long taskId = item.getRecheckRequest().getInventoryTask().getId();

        AssetInventoryFloor floor = floorRepository.findById(floorId)
                .filter(value -> taskId.equals(value.getInventoryTask().getId()))
                .filter(value -> locationId.equals(value.getLocation().getId()))
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Asset floor is outside this recheck location"
                ));

        return placeRepository.searchForApp(taskId, floor.getId(), normalizeSearch(search))
                .stream()
                .map(place -> new AppHierarchyOptionResponse(
                        place.getId(), null, place.getPlaceName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppHierarchyOptionResponse> sparePartLocations(
            Long requestId,
            Long itemId,
            String search,
            User user
    ) {
        InventoryRecheckItem item = requireOpenAssignedItem(
                requestId, itemId, InventoryDomain.SPARE_PART, user);
        Long branchId = requireId(
                item.getBranchId(), "Expected spare-part branch is unavailable");
        Long taskId = item.getRecheckRequest().getInventoryTask().getId();

        return spareLocationRepository.searchForApp(
                        taskId, branchId, normalizeSearch(search))
                .stream()
                .map(location -> new AppHierarchyOptionResponse(
                        location.getId(),
                        location.getLocationCode(),
                        location.getLocationCode()
                ))
                .toList();
    }

    private InventoryRecheckItem requireOpenAssignedItem(
            Long requestId,
            Long itemId,
            InventoryDomain domain,
            User user
    ) {
        accessPolicyService.assertCanUseApp(user);
        InventoryRecheckItem item = recheckItemRepository.findById(itemId)
                .filter(value -> requestId.equals(value.getRecheckRequest().getId()))
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "Assigned recheck item not found"));
        InventoryRecheckRequest request = item.getRecheckRequest();

        if (request.getAssignedTo() == null
                || user == null
                || user.getId() == null
                || !user.getId().equals(request.getAssignedTo().getId())) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND, "Assigned recheck item not found");
        }
        if (request.getInventoryDomain() != domain) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Recheck domain does not match the requested hierarchy");
        }
        if (request.getInventoryTask().getStatus() != InventoryTaskStatus.UNDER_REVIEW
                || !request.getStatus().isActive()
                || item.getStatus() != RecheckItemStatus.PENDING) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "Recheck item is not open");
        }
        return item;
    }

    private Long requireId(Long value, String message) {
        if (value == null) {
            throw new BusinessException(HttpStatus.CONFLICT, message);
        }
        return value;
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String value = search.trim();
        if (value.length() > 255) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "Search text is too long");
        }
        return value;
    }
}
