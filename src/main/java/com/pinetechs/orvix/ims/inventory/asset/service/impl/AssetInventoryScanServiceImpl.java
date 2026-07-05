package com.pinetechs.orvix.ims.inventory.asset.service.impl;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.asset.dto.AssetInventoryScanRequest;
import com.pinetechs.orvix.ims.inventory.asset.dto.AssetInventoryScanResponse;
import com.pinetechs.orvix.ims.inventory.asset.entity.*;
import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryItemStatus;
import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryScanResult;
import com.pinetechs.orvix.ims.inventory.asset.repository.*;
import com.pinetechs.orvix.ims.inventory.asset.service.AssetInventoryScanService;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AssetInventoryScanServiceImpl implements AssetInventoryScanService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final AssetInventoryItemRepository itemRepository;
    private final AssetInventoryLocationRepository locationRepository;
    private final AssetInventoryFloorRepository floorRepository;
    private final AssetInventoryPlaceRepository placeRepository;
    private final AssetInventoryLocationAssignmentRepository locationAssignmentRepository;
    private final AssetInventoryScanRepository scanRepository;

    public AssetInventoryScanServiceImpl(
            InventoryTaskRepository inventoryTaskRepository,
            AssetInventoryItemRepository itemRepository,
            AssetInventoryLocationRepository locationRepository,
            AssetInventoryFloorRepository floorRepository,
            AssetInventoryPlaceRepository placeRepository,
            AssetInventoryLocationAssignmentRepository locationAssignmentRepository,
            AssetInventoryScanRepository scanRepository
    ) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.floorRepository = floorRepository;
        this.placeRepository = placeRepository;
        this.locationAssignmentRepository = locationAssignmentRepository;
        this.scanRepository = scanRepository;
    }

    @Override
    @Transactional
    public AssetInventoryScanResponse scan(Long taskId, AssetInventoryScanRequest request, User currentUser) {
        InventoryTask task = inventoryTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task not found"));

        if (task.getInventoryDomain() != InventoryDomain.ASSET) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task is not an asset inventory task");
        }

        if (task.getStatus() != InventoryTaskStatus.READY_TO_START && task.getStatus() != InventoryTaskStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Asset scan is allowed only when task is READY_TO_START or IN_PROGRESS");
        }

        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Scan request is required");
        }

        String barcode = normalize(request.getBarcode());
        if (barcode == null || barcode.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Barcode is required");
        }

        AssetInventoryLocation actualLocation = getTaskLocation(taskId, request.getLocationId());
        AssetInventoryFloor actualFloor = getTaskFloor(taskId, actualLocation.getId(), request.getFloorId());
        AssetInventoryPlace actualPlace = getTaskPlace(taskId, actualLocation.getId(), actualFloor.getId(), request.getPlaceId());

        boolean assigned = locationAssignmentRepository.existsActiveByTaskIdAndUserIdAndLocationId(
                taskId,
                currentUser.getId(),
                actualLocation.getId()
        );

        if (!assigned) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "User is not assigned to this asset inventory location");
        }

        AssetInventoryItem item = itemRepository.findByInventoryTaskIdAndBarcode(taskId, barcode).orElse(null);

        AssetInventoryScan scan = new AssetInventoryScan();
        scan.setInventoryTask(task);
        scan.setItem(item);
        scan.setScannedBy(currentUser);
        scan.setScannedBarcode(barcode);
        scan.setActualLocation(actualLocation);
        scan.setActualFloor(actualFloor);
        scan.setActualPlace(actualPlace);
        scan.setNotes(request.getNotes());

        AssetInventoryScanResult result;
        String message;

        if (item == null) {
            result = AssetInventoryScanResult.EXTRA;
            message = "Barcode is not found in the uploaded asset inventory file";
            scan.setScanResult(result);
            AssetInventoryScan savedScan = scanRepository.save(scan);
            return toResponse(savedScan, null, result, null, message);
        }

        scan.setExpectedLocation(item.getPlannedLocation());
        scan.setExpectedFloor(item.getPlannedFloor());
        scan.setExpectedPlace(item.getPlannedPlace());

        if (item.getStatus() == AssetInventoryItemStatus.NOT_SCANNED) {
            boolean matched = isSamePlace(item.getPlannedLocation(), item.getPlannedFloor(), item.getPlannedPlace(), actualLocation, actualFloor, actualPlace);

            result = matched ? AssetInventoryScanResult.MATCHED : AssetInventoryScanResult.LOCATION_MISMATCH;

            item.setActualLocation(actualLocation);
            item.setActualFloor(actualFloor);
            item.setActualPlace(actualPlace);
            item.setCheckedBy(currentUser);
            item.setCheckedAt(LocalDateTime.now());
            item.setStatus(matched ? AssetInventoryItemStatus.MATCHED : AssetInventoryItemStatus.LOCATION_MISMATCH);

            task.setProcessedRecords(safe(task.getProcessedRecords()) + 1);
            actualLocation.setProcessedAssets(safe(actualLocation.getProcessedAssets()) + 1);

            if (matched) {
                task.setMatchedRecords(safe(task.getMatchedRecords()) + 1);
                actualLocation.setMatchedAssets(safe(actualLocation.getMatchedAssets()) + 1);
                message = "Asset matched the planned location";
            } else {
                message = "Asset was found, but in a different location/floor/place than planned";
            }

            itemRepository.save(item);
            task.setStatus(InventoryTaskStatus.IN_PROGRESS);
            inventoryTaskRepository.save(task);

        } else {
            boolean sameAsCurrentActual = isSamePlace(item.getActualLocation(), item.getActualFloor(), item.getActualPlace(), actualLocation, actualFloor, actualPlace);

            if (sameAsCurrentActual) {
                result = AssetInventoryScanResult.DUPLICATE_SAME_LOCATION;
                message = "Asset was already scanned in the same location";
            } else {
                result = AssetInventoryScanResult.DUPLICATE_DIFFERENT_LOCATION;
                item.setStatus(AssetInventoryItemStatus.DUPLICATE_REVIEW);
                itemRepository.save(item);
                message = "Asset was already scanned before in a different location and requires supervisor review";
            }
        }

        scan.setScanResult(result);
        AssetInventoryScan savedScan = scanRepository.save(scan);
        return toResponse(savedScan, item, result, item.getStatus(), message);
    }

    private AssetInventoryLocation getTaskLocation(Long taskId, Long locationId) {
        if (locationId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Location is required");
        }

        return locationRepository.findById(locationId)
                .filter(location -> location.getInventoryTask() != null && location.getInventoryTask().getId().equals(taskId))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Location does not belong to this asset inventory task"));
    }

    private AssetInventoryFloor getTaskFloor(Long taskId, Long locationId, Long floorId) {
        if (floorId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Floor is required");
        }

        return floorRepository.findById(floorId)
                .filter(floor -> floor.getInventoryTask() != null && floor.getInventoryTask().getId().equals(taskId))
                .filter(floor -> floor.getLocation() != null && floor.getLocation().getId().equals(locationId))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Floor does not belong to selected location"));
    }

    private AssetInventoryPlace getTaskPlace(Long taskId, Long locationId, Long floorId, Long placeId) {
        if (placeId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Place is required");
        }

        return placeRepository.findById(placeId)
                .filter(place -> place.getInventoryTask() != null && place.getInventoryTask().getId().equals(taskId))
                .filter(place -> place.getLocation() != null && place.getLocation().getId().equals(locationId))
                .filter(place -> place.getFloor() != null && place.getFloor().getId().equals(floorId))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Place does not belong to selected floor"));
    }

    private boolean isSamePlace(
            AssetInventoryLocation location1,
            AssetInventoryFloor floor1,
            AssetInventoryPlace place1,
            AssetInventoryLocation location2,
            AssetInventoryFloor floor2,
            AssetInventoryPlace place2
    ) {
        return sameId(location1, location2) && sameId(floor1, floor2) && sameId(place1, place2);
    }

    private boolean sameId(Object left, Object right) {
        if (left == null || right == null) {
            return left == right;
        }

        Long leftId = null;
        Long rightId = null;

        if (left instanceof AssetInventoryLocation location) {
            leftId = location.getId();
        } else if (left instanceof AssetInventoryFloor floor) {
            leftId = floor.getId();
        } else if (left instanceof AssetInventoryPlace place) {
            leftId = place.getId();
        }

        if (right instanceof AssetInventoryLocation location) {
            rightId = location.getId();
        } else if (right instanceof AssetInventoryFloor floor) {
            rightId = floor.getId();
        } else if (right instanceof AssetInventoryPlace place) {
            rightId = place.getId();
        }

        return leftId != null && leftId.equals(rightId);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private AssetInventoryScanResponse toResponse(
            AssetInventoryScan scan,
            AssetInventoryItem item,
            AssetInventoryScanResult result,
            AssetInventoryItemStatus itemStatus,
            String message
    ) {
        AssetInventoryScanResponse response = new AssetInventoryScanResponse();
        response.setScanId(scan.getId());
        response.setItemId(item == null ? null : item.getId());
        response.setBarcode(scan.getScannedBarcode());
        response.setScanResult(result);
        response.setItemStatus(itemStatus);
        response.setMessage(message);

        if (scan.getExpectedLocation() != null) {
            response.setExpectedLocationId(scan.getExpectedLocation().getId());
            response.setExpectedLocationName(scan.getExpectedLocation().getLocationName());
        }
        if (scan.getExpectedFloor() != null) {
            response.setExpectedFloorId(scan.getExpectedFloor().getId());
            response.setExpectedFloorName(scan.getExpectedFloor().getFloorName());
        }
        if (scan.getExpectedPlace() != null) {
            response.setExpectedPlaceId(scan.getExpectedPlace().getId());
            response.setExpectedPlaceName(scan.getExpectedPlace().getPlaceName());
        }
        if (scan.getActualLocation() != null) {
            response.setActualLocationId(scan.getActualLocation().getId());
            response.setActualLocationName(scan.getActualLocation().getLocationName());
        }
        if (scan.getActualFloor() != null) {
            response.setActualFloorId(scan.getActualFloor().getId());
            response.setActualFloorName(scan.getActualFloor().getFloorName());
        }
        if (scan.getActualPlace() != null) {
            response.setActualPlaceId(scan.getActualPlace().getId());
            response.setActualPlaceName(scan.getActualPlace().getPlaceName());
        }

        return response;
    }
}
