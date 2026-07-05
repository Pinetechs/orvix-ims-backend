package com.pinetechs.orvix.ims.inventory.vehicle.service.impl;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.dto.AssignInventoryTaskStaffRequest;
import com.pinetechs.orvix.ims.inventory.task.dto.InventoryTaskAssignmentResponse;
import com.pinetechs.orvix.ims.inventory.task.dto.StaffLocationAssignmentRequest;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryItemResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryLocationResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocation;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocationAssignment;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.service.VehicleInventoryQueryService;
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
public class VehicleInventoryQueryServiceImpl implements VehicleInventoryQueryService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final VehicleInventoryItemRepository itemRepository;
    private final VehicleInventoryLocationRepository locationRepository;
    private final AccessPolicyService accessPolicyService;
    private final UserRepository userRepository;
    private final InventoryTaskAssignmentRepository assignmentRepository;
    private final VehicleInventoryLocationAssignmentRepository locationAssignmentRepository;


    public VehicleInventoryQueryServiceImpl(InventoryTaskRepository taskRepository, VehicleInventoryItemRepository itemRepository, VehicleInventoryLocationRepository locationRepository,
                                            AccessPolicyService accessPolicyService, UserRepository userRepository, InventoryTaskAssignmentRepository assignmentRepository, VehicleInventoryLocationAssignmentRepository locationAssignmentRepository) {
        this.inventoryTaskRepository = taskRepository;
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.accessPolicyService = accessPolicyService;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.locationAssignmentRepository = locationAssignmentRepository;
    }

    @Override
    public Page<VehicleInventoryItemResponse> getTaskItems(Long taskId, User currentUser, Pageable pageable) {
        InventoryTask task = inventoryTaskRepository.findById(taskId).orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,"Inventory task not found"));
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);

        return itemRepository.findByInventoryTask_Id(taskId, pageable).map(VehicleInventoryItemResponse::from);
    }

    @Override
    public List<VehicleInventoryLocationResponse> getTaskLocations(Long taskId, User currentUser) {
        InventoryTask task = inventoryTaskRepository.findById(taskId).orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,"Inventory task not found"));
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);

        return locationRepository.findByInventoryTaskId(taskId)
                .stream()
                .map(VehicleInventoryLocationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTaskAssignmentResponse> getAssignments(Long taskId, User currentUser) {
        InventoryTask task = getTask(taskId);

        accessPolicyService.assertCanViewInventoryTask(currentUser, task);

        return assignmentRepository.findActiveByInventoryTaskIdWithUser(taskId)
                .stream()
                .map(assignment -> InventoryTaskAssignmentResponse.from(
                        assignment,
                        locationAssignmentRepository.findActiveByAssignmentIdWithLocation(assignment.getId())
                ))
                .toList();
    }


    @Override
    @Transactional
    public List<InventoryTaskAssignmentResponse> assignStaff(Long taskId, AssignInventoryTaskStaffRequest request, User currentUser) {
        InventoryTask task = getTask(taskId);

        InventoryTaskStatus inventoryTaskStatus = request.getTaskStatus();

        accessPolicyService.assertCanAssignInventoryTaskUsers(currentUser, task.getCompany().getId(), task.getInventoryDomain());

        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "At least one inventory staff user is required");
        }

        List<Long> userIds = normalizeIds(request.getUserIds());
        if (userIds.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "At least one inventory staff user is required");
        }
        if (inventoryTaskStatus == null){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task status is required");
        }

        if (inventoryTaskStatus != InventoryTaskStatus.READY_TO_START && inventoryTaskStatus != InventoryTaskStatus.PAUSED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task must be in READY_TO_START or PAUSED status to assign staff");
        }



        Map<Long, User> staffById = loadAndValidateStaffUsers(task, userIds);
        Map<Long, List<Long>> requestedLocationIdsByUserId = normalizeLocationAssignments(request.getLocationAssignments(), userIds);
        Map<Long, VehicleInventoryLocation> taskLocationsById = loadTaskLocationsById(task);

        if (task.getInventoryDomain() == InventoryDomain.VEHICLE && !requestedLocationIdsByUserId.isEmpty()) {
            validateRequestedLocationsBelongToTask(requestedLocationIdsByUserId, taskLocationsById);
        }

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
                VehicleInventoryLocationAssignment locationAssignment = new VehicleInventoryLocationAssignment();
                locationAssignment.setAssignment(savedAssignment);
                locationAssignment.setLocation(taskLocationsById.get(locationId));
                locationAssignment.setActive(true);
                locationAssignmentRepository.save(locationAssignment);
            }
        }

        task.setStatus(inventoryTaskStatus);
        inventoryTaskRepository.save(task);

        return savedAssignments.stream()
                .map(assignment -> InventoryTaskAssignmentResponse.from(
                        assignment,
                        locationAssignmentRepository.findActiveByAssignmentIdWithLocation(assignment.getId())
                ))
                .toList();
    }


    private InventoryTask getTask(Long taskId) {
        return inventoryTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Inventory task not found"));
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

            /*if (!user.hasInventoryDomain(task.getInventoryDomain())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "User " + user.getUsername() + " is not allowed for " + task.getInventoryDomain() + " inventory");
            }*/
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

    private Map<Long, VehicleInventoryLocation> loadTaskLocationsById(InventoryTask task) {
        if (task.getInventoryDomain() != InventoryDomain.VEHICLE) {
            return new HashMap<>();
        }

        return locationRepository.findByInventoryTaskId(task.getId())
                .stream()
                .collect(Collectors.toMap(VehicleInventoryLocation::getId, location -> location));
    }

    private void validateRequestedLocationsBelongToTask(
            Map<Long, List<Long>> requestedLocationIdsByUserId,
            Map<Long, VehicleInventoryLocation> taskLocationsById
    ) {
        Set<Long> taskLocationIds = taskLocationsById.keySet();

        for (List<Long> locationIds : requestedLocationIdsByUserId.values()) {
            for (Long locationId : locationIds) {
                if (!taskLocationIds.contains(locationId)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "Location does not belong to this inventory task. locationId=" + locationId);
                }
            }
        }
    }

}
