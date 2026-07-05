package com.pinetechs.orvix.ims.inventory.asset.service.impl;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.asset.dto.*;
import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryLocation;
import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryLocationAssignment;
import com.pinetechs.orvix.ims.inventory.asset.repository.*;
import com.pinetechs.orvix.ims.inventory.asset.service.AssetInventoryQueryService;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.dto.AssignInventoryTaskStaffRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.StaffLocationAssignmentRequest;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.UserType;
import com.pinetechs.orvix.ims.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssetInventoryQueryServiceImpl implements AssetInventoryQueryService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final AssetInventoryItemRepository itemRepository;
    private final AssetInventoryLocationRepository locationRepository;
    private final AssetInventoryFloorRepository floorRepository;
    private final AssetInventoryPlaceRepository placeRepository;
    private final AssetInventoryCategoryRepository categoryRepository;
    private final AccessPolicyService accessPolicyService;
    private final UserRepository userRepository;
    private final InventoryTaskAssignmentRepository assignmentRepository;
    private final AssetInventoryLocationAssignmentRepository locationAssignmentRepository;

    public AssetInventoryQueryServiceImpl(
            InventoryTaskRepository inventoryTaskRepository,
            AssetInventoryItemRepository itemRepository,
            AssetInventoryLocationRepository locationRepository,
            AssetInventoryFloorRepository floorRepository,
            AssetInventoryPlaceRepository placeRepository,
            AssetInventoryCategoryRepository categoryRepository,
            AccessPolicyService accessPolicyService,
            UserRepository userRepository,
            InventoryTaskAssignmentRepository assignmentRepository,
            AssetInventoryLocationAssignmentRepository locationAssignmentRepository
    ) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.floorRepository = floorRepository;
        this.placeRepository = placeRepository;
        this.categoryRepository = categoryRepository;
        this.accessPolicyService = accessPolicyService;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.locationAssignmentRepository = locationAssignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetInventoryItemResponse> getTaskItems(Long taskId, User currentUser, Pageable pageable) {
        InventoryTask task = getAssetTask(taskId);
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);

        return itemRepository.findByInventoryTask_Id(taskId, pageable).map(AssetInventoryItemResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetInventoryLocationResponse> getTaskLocations(Long taskId, User currentUser) {
        InventoryTask task = getAssetTask(taskId);
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);

        return locationRepository.findByInventoryTaskId(taskId)
                .stream()
                .map(AssetInventoryLocationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetInventoryLocationResponse> getMyAssignedLocations(Long taskId, User currentUser) {
        InventoryTask task = getAssetTask(taskId);

        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return locationAssignmentRepository.findActiveByTaskIdAndUserIdWithLocation(task.getId(), currentUser.getId())
                .stream()
                .map(AssetInventoryLocationAssignment::getLocation)
                .map(AssetInventoryLocationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetInventoryFloorResponse> getLocationFloors(Long taskId, Long locationId, User currentUser) {
        InventoryTask task = getAssetTask(taskId);
        AssetInventoryLocation location = getTaskLocation(taskId, locationId);
        assertCanViewLocation(currentUser, task, location.getId());

        return floorRepository.findByLocationIdOrderByFloorNameAsc(location.getId())
                .stream()
                .map(AssetInventoryFloorResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetInventoryPlaceResponse> getFloorPlaces(Long taskId, Long floorId, User currentUser) {
        InventoryTask task = getAssetTask(taskId);

        return floorRepository.findById(floorId)
                .filter(floor -> floor.getInventoryTask() != null && floor.getInventoryTask().getId().equals(taskId))
                .map(floor -> {
                    assertCanViewLocation(currentUser, task, floor.getLocation().getId());
                    return placeRepository.findByFloorIdOrderByPlaceNameAsc(floor.getId())
                        .stream()
                        .map(AssetInventoryPlaceResponse::from)
                        .toList();
                })
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Floor does not belong to this asset inventory task"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetInventoryCategoryResponse> getTaskCategories(Long taskId, User currentUser) {
        InventoryTask task = getAssetTask(taskId);
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);

        return categoryRepository.findByInventoryTaskId(taskId)
                .stream()
                .map(AssetInventoryCategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetInventoryAssignmentResponse> getAssignments(Long taskId, User currentUser) {
        InventoryTask task = getAssetTask(taskId);
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);

        return assignmentRepository.findActiveByInventoryTaskIdWithUser(taskId)
                .stream()
                .map(assignment -> AssetInventoryAssignmentResponse.from(
                        assignment,
                        locationAssignmentRepository.findActiveByAssignmentIdWithLocation(assignment.getId())
                ))
                .toList();
    }

    @Override
    @Transactional
    public List<AssetInventoryAssignmentResponse> assignStaff(Long taskId, AssignInventoryTaskStaffRequest request, User currentUser) {
        InventoryTask task = getAssetTask(taskId);


        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "At least one inventory staff user is required");
        }

        InventoryTaskStatus inventoryTaskStatus = request.getTaskStatus();
        if (inventoryTaskStatus == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task status is required");
        }
        if (inventoryTaskStatus == InventoryTaskStatus.DRAFT){
            inventoryTaskStatus = InventoryTaskStatus.IMPORT_COMPLETED;
        }

        if (inventoryTaskStatus != InventoryTaskStatus.READY_TO_START && inventoryTaskStatus != InventoryTaskStatus.IMPORT_COMPLETED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task must be in READY_TO_START or IMPORT COMPLETED status to assign staff");
        }

        accessPolicyService.assertCanAssignInventoryTaskUsers(currentUser, task.getCompany().getId(), InventoryDomain.ASSET);

        List<Long> userIds = normalizeIds(request.getUserIds());
        if (userIds.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "At least one inventory staff user is required");
        }

        Map<Long, User> staffById = loadAndValidateStaffUsers(task, userIds);
        Map<Long, List<Long>> requestedLocationIdsByUserId = normalizeLocationAssignments(request.getLocationAssignments(), userIds);
        validateEveryUserHasAtLeastOneLocation(userIds, requestedLocationIdsByUserId);
        Map<Long, AssetInventoryLocation> taskLocationsById = loadTaskLocationsById(task);

        validateRequestedLocationsBelongToTask(requestedLocationIdsByUserId, taskLocationsById);

        locationAssignmentRepository.deleteByTaskId(taskId);
        assignmentRepository.deleteByInventoryTaskId(taskId);

        List<InventoryTaskAssignment> savedAssignments = new ArrayList<>();

        for (Long userId : userIds) {
            InventoryTaskAssignment assignment = new InventoryTaskAssignment();
            assignment.setInventoryTask(task);
            assignment.setUser(staffById.get(userId));
            assignment.setAssignedBy(currentUser);
            assignment.setActive(true);

            InventoryTaskAssignment savedAssignment = assignmentRepository.save(assignment);
            savedAssignments.add(savedAssignment);

            List<Long> locationIds = requestedLocationIdsByUserId.getOrDefault(userId, List.of());

            for (Long locationId : locationIds) {
                AssetInventoryLocationAssignment locationAssignment = new AssetInventoryLocationAssignment();
                locationAssignment.setAssignment(savedAssignment);
                locationAssignment.setLocation(taskLocationsById.get(locationId));
                locationAssignment.setActive(true);
                locationAssignmentRepository.save(locationAssignment);
            }
        }

        task.setStatus(inventoryTaskStatus);
        inventoryTaskRepository.save(task);

        return savedAssignments.stream()
                .map(assignment -> AssetInventoryAssignmentResponse.from(
                        assignment,
                        locationAssignmentRepository.findActiveByAssignmentIdWithLocation(assignment.getId())
                ))
                .toList();
    }

    private void assertCanViewLocation(User currentUser, InventoryTask task, Long locationId) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        if (locationAssignmentRepository.existsActiveByTaskIdAndUserIdAndLocationId(task.getId(), currentUser.getId(), locationId)) {
            return;
        }

        accessPolicyService.assertCanViewInventoryTask(currentUser, task);
    }

    private InventoryTask getAssetTask(Long taskId) {
        InventoryTask task = inventoryTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task not found"));

        if (task.getInventoryDomain() != InventoryDomain.ASSET) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task is not an asset inventory task");
        }

        return task;
    }

    private AssetInventoryLocation getTaskLocation(Long taskId, Long locationId) {
        return locationRepository.findById(locationId)
                .filter(location -> location.getInventoryTask() != null && location.getInventoryTask().getId().equals(taskId))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Location does not belong to this asset inventory task"));
    }

    private List<Long> normalizeIds(List<Long> ids) {
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<Long, User> loadAndValidateStaffUsers(InventoryTask task, List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);

        if (users.size() != userIds.size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "One or more selected inventory staff users do not exist");
        }

        Map<Long, User> usersById = users.stream().collect(Collectors.toMap(User::getId, user -> user));

        for (Long userId : userIds) {
            User user = usersById.get(userId);

            if (user.getUserType() != UserType.INVENTORY_STAFF) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "User " + user.getUsername() + " is not inventory staff");
            }

            if (!Boolean.TRUE.equals(user.getEnabled()) || Boolean.TRUE.equals(user.getDeleted())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "User " + user.getUsername() + " is not active");
            }

            if (!user.hasCompany(task.getCompany().getId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "User " + user.getUsername() + " is not linked to the task company");
            }
        }

        return usersById;
    }

    private Map<Long, List<Long>> normalizeLocationAssignments(List<StaffLocationAssignmentRequest> locationAssignments, List<Long> allowedUserIds) {
        if (locationAssignments == null || locationAssignments.isEmpty()) {
            return new HashMap<>();
        }

        Set<Long> allowedUserIdSet = new HashSet<>(allowedUserIds);
        Map<Long, List<Long>> locationIdsByUserId = new LinkedHashMap<>();

        for (StaffLocationAssignmentRequest assignmentRequest : locationAssignments) {
            if (assignmentRequest == null || assignmentRequest.getUserId() == null) {
                continue;
            }

            if (!allowedUserIdSet.contains(assignmentRequest.getUserId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Location assignment contains a user that is not selected as staff");
            }

            List<Long> locationIds = assignmentRequest.getLocationIds() == null
                    ? List.of()
                    : assignmentRequest.getLocationIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            locationIdsByUserId.put(assignmentRequest.getUserId(), locationIds);
        }

        return locationIdsByUserId;
    }

    private void validateEveryUserHasAtLeastOneLocation(List<Long> userIds, Map<Long, List<Long>> requestedLocationIdsByUserId) {
        for (Long userId : userIds) {
            List<Long> locationIds = requestedLocationIdsByUserId.getOrDefault(userId, List.of());
            if (locationIds.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Each asset inventory staff user must have at least one assigned location. userId=" + userId);
            }
        }
    }

    private Map<Long, AssetInventoryLocation> loadTaskLocationsById(InventoryTask task) {
        return locationRepository.findByInventoryTaskId(task.getId())
                .stream()
                .collect(Collectors.toMap(AssetInventoryLocation::getId, location -> location));
    }

    private void validateRequestedLocationsBelongToTask(
            Map<Long, List<Long>> requestedLocationIdsByUserId,
            Map<Long, AssetInventoryLocation> taskLocationsById
    ) {
        Set<Long> taskLocationIds = taskLocationsById.keySet();

        for (List<Long> locationIds : requestedLocationIdsByUserId.values()) {
            for (Long locationId : locationIds) {
                if (!taskLocationIds.contains(locationId)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "Location does not belong to this asset inventory task. locationId=" + locationId);
                }
            }
        }
    }
}
