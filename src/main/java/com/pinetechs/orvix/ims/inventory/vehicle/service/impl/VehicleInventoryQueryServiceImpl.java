package com.pinetechs.orvix.ims.inventory.vehicle.service.impl;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.dto.AssignInventoryTaskStaffLocationRequest;
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
    public List<InventoryTaskAssignmentResponse> assignStaff(Long taskId, AssignInventoryTaskStaffLocationRequest request, User currentUser) {
        InventoryTask task = getTaskForAssignmentUpdate(taskId);

        validateTaskCanBeAssigned(task);

        InventoryTaskStatus nextStatus = resolveAssignmentTargetStatus(task, request);

        accessPolicyService.assertCanAssignInventoryTaskUsers(currentUser, task.getCompany().getId(), InventoryDomain.VEHICLE);

        Map<Long, List<Long>> requestedLocationIdsByUserId = normalizeLocationAssignments(
                request.getLocationAssignments(),
                "vehicle"
        );

        validateEveryUserHasAtLeastOneLocation(requestedLocationIdsByUserId, "vehicle");

        List<Long> userIds = new ArrayList<>(requestedLocationIdsByUserId.keySet());
        Map<Long, User> staffById = loadAndValidateStaffUsers(task, userIds);

        Map<Long, VehicleInventoryLocation> taskLocationsById = loadTaskLocationsById(task);
        validateRequestedLocationsBelongToTask(requestedLocationIdsByUserId, taskLocationsById);

        List<InventoryTaskAssignment> savedAssignments = synchronizeAssignments(
                task,
                currentUser,
                requestedLocationIdsByUserId,
                staffById,
                taskLocationsById
        );

        if (task.getStatus() != nextStatus) {
            task.setStatus(nextStatus);
            inventoryTaskRepository.save(task);
        }

        return savedAssignments.stream()
                .map(assignment -> InventoryTaskAssignmentResponse.from(
                        assignment,
                        locationAssignmentRepository.findActiveByAssignmentIdWithLocation(assignment.getId())
                ))
                .toList();
    }


    private InventoryTask getTask(Long taskId) {
        InventoryTask task = inventoryTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task not found"));

        if (task.getInventoryDomain() != InventoryDomain.VEHICLE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task is not a vehicle inventory task");
        }

        return task;
    }

    private InventoryTask getTaskForAssignmentUpdate(Long taskId) {
        InventoryTask task = inventoryTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task not found"));
        if (task.getInventoryDomain() != InventoryDomain.VEHICLE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task is not a vehicle inventory task");
        }
        return task;
    }

    private void validateTaskCanBeAssigned(InventoryTask task) {
        if (task.getStatus() != InventoryTaskStatus.IMPORT_COMPLETED
                && task.getStatus() != InventoryTaskStatus.READY_FOR_ASSIGNMENT
                && task.getStatus() != InventoryTaskStatus.READY_TO_START
                && task.getStatus() != InventoryTaskStatus.IN_PROGRESS
                && task.getStatus() != InventoryTaskStatus.PAUSED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Vehicle inventory assignments cannot be changed in task status " + task.getStatus()
            );
        }
    }

    private InventoryTaskStatus resolveAssignmentTargetStatus(
            InventoryTask task,
            AssignInventoryTaskStaffLocationRequest request
    ) {
        if (request == null || request.getLocationAssignments() == null || request.getLocationAssignments().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "At least one vehicle inventory staff assignment is required");
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
                throw new BusinessException(HttpStatus.BAD_REQUEST, "User " + user.getUsername() + " is not enabled for vehicle inventory");
            }
        }

        return usersById;
    }

    private List<InventoryTaskAssignment> synchronizeAssignments(
            InventoryTask task,
            User currentUser,
            Map<Long, List<Long>> requestedLocationIdsByUserId,
            Map<Long, User> staffById,
            Map<Long, VehicleInventoryLocation> taskLocationsById
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
            Map<Long, VehicleInventoryLocation> taskLocationsById
    ) {
        Map<Long, VehicleInventoryLocationAssignment> existingByLocationId = locationAssignmentRepository
                .findByAssignmentId(assignment.getId())
                .stream()
                .collect(Collectors.toMap(value -> value.getLocation().getId(), value -> value));

        for (Map.Entry<Long, VehicleInventoryLocationAssignment> entry : existingByLocationId.entrySet()) {
            entry.getValue().setActive(requestedLocationIds.contains(entry.getKey()));
        }
        locationAssignmentRepository.saveAll(existingByLocationId.values());

        for (Long locationId : requestedLocationIds) {
            if (!existingByLocationId.containsKey(locationId)) {
                VehicleInventoryLocationAssignment locationAssignment = new VehicleInventoryLocationAssignment();
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

    private Map<Long, VehicleInventoryLocation> loadTaskLocationsById(InventoryTask task) {
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
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "Location does not belong to this vehicle inventory task. locationId=" + locationId);
                }
            }
        }
    }

}
