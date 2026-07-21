package com.pinetechs.orvix.ims.inventory.asset.service.impl;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.asset.dto.*;
import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryLocation;
import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryLocationAssignment;
import com.pinetechs.orvix.ims.inventory.asset.repository.*;
import com.pinetechs.orvix.ims.inventory.asset.service.AssetInventoryQueryService;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.dto.AssignInventoryTaskStaffLocationRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.StaffLocationAssignmentRequest;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.task.enums.InventoryTaskActivityType;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskActivityService;
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
    private final InventoryTaskActivityService taskActivityService;

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
            AssetInventoryLocationAssignmentRepository locationAssignmentRepository,
            InventoryTaskActivityService taskActivityService
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
        this.taskActivityService = taskActivityService;
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
    public List<AssetInventoryAssignmentResponse> assignStaff(Long taskId, AssignInventoryTaskStaffLocationRequest request, User currentUser) {
        InventoryTask task = getAssetTaskForAssignmentUpdate(taskId);

        validateTaskCanBeAssigned(task);

        InventoryTaskStatus nextStatus = resolveAssignmentTargetStatus(task, request);

        accessPolicyService.assertCanAssignInventoryTaskUsers(currentUser, task.getCompany().getId(), InventoryDomain.ASSET);

        Map<Long, List<Long>> requestedLocationIdsByUserId = normalizeLocationAssignments(
                request.getLocationAssignments(),
                "asset"
        );

        validateEveryUserHasAtLeastOneLocation(requestedLocationIdsByUserId, "asset");

        List<Long> userIds = new ArrayList<>(requestedLocationIdsByUserId.keySet());
        Map<Long, User> staffById = loadAndValidateStaffUsers(task, userIds);

        Map<Long, AssetInventoryLocation> taskLocationsById = loadTaskLocationsById(task);
        validateRequestedLocationsBelongToTask(requestedLocationIdsByUserId, taskLocationsById);

        List<InventoryTaskAssignment> savedAssignments = synchronizeAssignments(
                task,
                currentUser,
                requestedLocationIdsByUserId,
                staffById,
                taskLocationsById
        );

        InventoryTaskStatus fromStatus = task.getStatus();
        if (fromStatus != nextStatus) {
            task.setStatus(nextStatus);
            inventoryTaskRepository.save(task);
        }

        int locationLinkCount = requestedLocationIdsByUserId.values().stream()
                .mapToInt(List::size)
                .sum();
        taskActivityService.record(
                task,
                InventoryTaskActivityType.ASSIGNMENTS_UPDATED,
                fromStatus,
                nextStatus,
                currentUser,
                null,
                "staff=" + requestedLocationIdsByUserId.size()
                        + ", locationAssignments=" + locationLinkCount
        );

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

    private InventoryTask getAssetTaskForAssignmentUpdate(Long taskId) {
        InventoryTask task = inventoryTaskRepository.findByIdForUpdate(taskId)
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

    private void validateTaskCanBeAssigned(InventoryTask task) {
        if (task.getStatus() != InventoryTaskStatus.IMPORT_COMPLETED
                && task.getStatus() != InventoryTaskStatus.READY_FOR_ASSIGNMENT
                && task.getStatus() != InventoryTaskStatus.READY_TO_START
                && task.getStatus() != InventoryTaskStatus.IN_PROGRESS
                && task.getStatus() != InventoryTaskStatus.PAUSED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Asset inventory assignments cannot be changed in task status " + task.getStatus()
            );
        }
    }

    private InventoryTaskStatus resolveAssignmentTargetStatus(
            InventoryTask task,
            AssignInventoryTaskStaffLocationRequest request
    ) {
        if (request == null || request.getLocationAssignments() == null || request.getLocationAssignments().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "At least one asset inventory staff assignment is required");
        }

        if (task.getStatus() == InventoryTaskStatus.IN_PROGRESS
                || task.getStatus() == InventoryTaskStatus.PAUSED) {
            return task.getStatus();
        }

        InventoryTaskStatus requestedStatus = request.getTaskStatus();
        if (requestedStatus == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task status is required");
        }

        if (requestedStatus == InventoryTaskStatus.DRAFT) {
            return InventoryTaskStatus.IMPORT_COMPLETED;
        }

        if (requestedStatus != InventoryTaskStatus.IMPORT_COMPLETED
                && requestedStatus != InventoryTaskStatus.READY_TO_START) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Task status must be IMPORT_COMPLETED or READY_TO_START"
            );
        }

        return requestedStatus;
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
            if (!user.hasInventoryDomain(task.getInventoryDomain())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "User " + user.getUsername() + " is not enabled for asset inventory");
            }
        }

        return usersById;
    }

    private List<InventoryTaskAssignment> synchronizeAssignments(
            InventoryTask task,
            User currentUser,
            Map<Long, List<Long>> requestedLocationIdsByUserId,
            Map<Long, User> staffById,
            Map<Long, AssetInventoryLocation> taskLocationsById
    ) {
        Map<Long, InventoryTaskAssignment> existingByUserId = assignmentRepository
                .findByInventoryTaskId(task.getId())
                .stream()
                .collect(Collectors.toMap(assignment -> assignment.getUser().getId(), assignment -> assignment));

        for (Map.Entry<Long, InventoryTaskAssignment> entry : existingByUserId.entrySet()) {
            entry.getValue().setActive(requestedLocationIdsByUserId.containsKey(entry.getKey()));
        }
        assignmentRepository.saveAll(existingByUserId.values());

        List<InventoryTaskAssignment> activeAssignments = new ArrayList<>();
        for (Map.Entry<Long, List<Long>> requested : requestedLocationIdsByUserId.entrySet()) {
            Long userId = requested.getKey();
            InventoryTaskAssignment assignment = existingByUserId.get(userId);
            if (assignment == null) {
                assignment = new InventoryTaskAssignment();
                assignment.setInventoryTask(task);
                assignment.setUser(staffById.get(userId));
            }
            assignment.setAssignedBy(currentUser);
            assignment.setActive(true);
            assignment = assignmentRepository.save(assignment);
            synchronizeLocationAssignments(
                    assignment,
                    new LinkedHashSet<>(requested.getValue()),
                    taskLocationsById
            );
            activeAssignments.add(assignment);
        }
        return activeAssignments;
    }

    private void synchronizeLocationAssignments(
            InventoryTaskAssignment assignment,
            Set<Long> requestedLocationIds,
            Map<Long, AssetInventoryLocation> taskLocationsById
    ) {
        Map<Long, AssetInventoryLocationAssignment> existingByLocationId = locationAssignmentRepository
                .findByAssignmentId(assignment.getId())
                .stream()
                .collect(Collectors.toMap(value -> value.getLocation().getId(), value -> value));

        for (Map.Entry<Long, AssetInventoryLocationAssignment> entry : existingByLocationId.entrySet()) {
            entry.getValue().setActive(requestedLocationIds.contains(entry.getKey()));
        }
        locationAssignmentRepository.saveAll(existingByLocationId.values());

        for (Long locationId : requestedLocationIds) {
            if (!existingByLocationId.containsKey(locationId)) {
                AssetInventoryLocationAssignment locationAssignment = new AssetInventoryLocationAssignment();
                locationAssignment.setAssignment(assignment);
                locationAssignment.setLocation(taskLocationsById.get(locationId));
                locationAssignment.setActive(true);
                locationAssignmentRepository.save(locationAssignment);
            }
        }
    }

    private Map<Long, List<Long>> normalizeLocationAssignments(
            List<StaffLocationAssignmentRequest> locationAssignments,
            String domainName
    ) {
        if (locationAssignments == null || locationAssignments.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "At least one " + domainName + " inventory staff assignment is required");
        }

        Map<Long, List<Long>> locationIdsByUserId = new LinkedHashMap<>();

        for (StaffLocationAssignmentRequest assignmentRequest : locationAssignments) {
            if (assignmentRequest == null || assignmentRequest.getUserId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "User id is required in location assignment");
            }

            Long userId = assignmentRequest.getUserId();

            if (locationIdsByUserId.containsKey(userId)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Duplicate staff location assignment is not allowed. userId=" + userId);
            }

            List<Long> locationIds = assignmentRequest.getLocationIds() == null
                    ? List.of()
                    : assignmentRequest.getLocationIds()
                    .stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            locationIdsByUserId.put(userId, locationIds);
        }

        return locationIdsByUserId;
    }

    private void validateEveryUserHasAtLeastOneLocation(
            Map<Long, List<Long>> requestedLocationIdsByUserId,
            String domainName
    ) {
        if (requestedLocationIdsByUserId == null || requestedLocationIdsByUserId.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "At least one " + domainName + " inventory staff assignment is required");
        }

        for (Map.Entry<Long, List<Long>> entry : requestedLocationIdsByUserId.entrySet()) {
            Long userId = entry.getKey();
            List<Long> locationIds = entry.getValue();

            if (locationIds == null || locationIds.isEmpty()) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Each " + domainName + " inventory staff user must have at least one assigned location. userId=" + userId
                );
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
