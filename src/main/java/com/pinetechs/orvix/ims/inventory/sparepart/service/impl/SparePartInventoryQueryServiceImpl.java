package com.pinetechs.orvix.ims.inventory.sparepart.service.impl;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.sparepart.dto.*;
import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryBranch;
import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryBranchAssignment;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.*;
import com.pinetechs.orvix.ims.inventory.sparepart.service.SparePartInventoryQueryService;
import com.pinetechs.orvix.ims.inventory.task.dto.AssignInventoryTaskStaffBranchRequest;
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
public class SparePartInventoryQueryServiceImpl implements SparePartInventoryQueryService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final SparePartInventoryItemRepository itemRepository;
    private final SparePartInventoryBranchRepository branchRepository;
    private final SparePartInventoryLocationRepository locationRepository;
    private final SparePartInventoryBrandRepository brandRepository;
    private final AccessPolicyService accessPolicyService;
    private final UserRepository userRepository;
    private final InventoryTaskAssignmentRepository assignmentRepository;
    private final SparePartInventoryBranchAssignmentRepository branchAssignmentRepository;
    private final InventoryTaskActivityService taskActivityService;

    public SparePartInventoryQueryServiceImpl(
            InventoryTaskRepository inventoryTaskRepository,
            SparePartInventoryItemRepository itemRepository,
            SparePartInventoryBranchRepository branchRepository,
            SparePartInventoryLocationRepository locationRepository,
            SparePartInventoryBrandRepository brandRepository,
            AccessPolicyService accessPolicyService,
            UserRepository userRepository,
            InventoryTaskAssignmentRepository assignmentRepository,
            SparePartInventoryBranchAssignmentRepository branchAssignmentRepository,
            InventoryTaskActivityService taskActivityService
    ) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.itemRepository = itemRepository;
        this.branchRepository = branchRepository;
        this.locationRepository = locationRepository;
        this.brandRepository = brandRepository;
        this.accessPolicyService = accessPolicyService;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.branchAssignmentRepository = branchAssignmentRepository;
        this.taskActivityService = taskActivityService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SparePartInventoryItemResponse> getTaskItems(Long taskId, User currentUser, Pageable pageable) {
        InventoryTask task = getSparePartTask(taskId);
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);
        return itemRepository.findByInventoryTask_Id(taskId, pageable).map(SparePartInventoryItemResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SparePartInventoryBranchResponse> getTaskBranches(Long taskId, User currentUser) {
        InventoryTask task = getSparePartTask(taskId);
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);
        return branchRepository.findByInventoryTaskId(taskId).stream()
                .map(SparePartInventoryBranchResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SparePartInventoryBranchResponse> getMyAssignedBranches(Long taskId, User currentUser) {
        InventoryTask task = getSparePartTask(taskId);
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return branchAssignmentRepository.findActiveByTaskIdAndUserIdWithBranch(task.getId(), currentUser.getId())
                .stream()
                .map(SparePartInventoryBranchAssignment::getBranch)
                .map(SparePartInventoryBranchResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SparePartInventoryLocationResponse> getBranchLocations(Long taskId, Long branchId, User currentUser) {
        InventoryTask task = getSparePartTask(taskId);
        SparePartInventoryBranch branch = getTaskBranch(taskId, branchId);
        assertCanViewBranch(currentUser, task, branch.getId());
        return locationRepository.findByBranchIdOrderByLocationCodeAsc(branch.getId())
                .stream()
                .map(SparePartInventoryLocationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SparePartInventoryBrandResponse> getTaskBrands(Long taskId, User currentUser) {
        InventoryTask task = getSparePartTask(taskId);
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);
        return brandRepository.findByInventoryTaskId(taskId).stream()
                .map(SparePartInventoryBrandResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SparePartInventoryAssignmentResponse> getAssignments(Long taskId, User currentUser) {
        InventoryTask task = getSparePartTask(taskId);
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);
        return assignmentRepository.findActiveByInventoryTaskIdWithUser(taskId)
                .stream()
                .map(assignment -> SparePartInventoryAssignmentResponse.from(
                        assignment,
                        branchAssignmentRepository.findActiveByAssignmentIdWithBranch(assignment.getId())
                ))
                .toList();
    }


    @Override
    @Transactional
    public List<SparePartInventoryAssignmentResponse> assignStaff(Long taskId, AssignInventoryTaskStaffBranchRequest request, User currentUser) {
        InventoryTask task = getSparePartTaskForAssignmentUpdate(taskId);

        validateTaskCanBeAssigned(task);

        InventoryTaskStatus nextStatus = resolveAssignmentTargetStatus(task, request);

        accessPolicyService.assertCanAssignInventoryTaskUsers(currentUser, task.getCompany().getId(), InventoryDomain.SPARE_PART);

        Map<Long, List<Long>> requestedBranchIdsByUserId = normalizeBranchAssignments(request.getBranchAssignments());

        validateEveryUserHasAtLeastOneBranch(requestedBranchIdsByUserId);

        List<Long> userIds = new ArrayList<>(requestedBranchIdsByUserId.keySet());
        Map<Long, User> staffById = loadAndValidateStaffUsers(task, userIds);

        Map<Long, SparePartInventoryBranch> taskBranchesById = loadTaskBranchesById(task);
        validateRequestedBranchesBelongToTask(requestedBranchIdsByUserId, taskBranchesById);

        List<InventoryTaskAssignment> savedAssignments = synchronizeAssignments(
                task,
                currentUser,
                requestedBranchIdsByUserId,
                staffById,
                taskBranchesById
        );

        InventoryTaskStatus fromStatus = task.getStatus();
        if (fromStatus != nextStatus) {
            task.setStatus(nextStatus);
            inventoryTaskRepository.save(task);
        }

        int branchLinkCount = requestedBranchIdsByUserId.values().stream()
                .mapToInt(List::size)
                .sum();
        taskActivityService.record(
                task,
                InventoryTaskActivityType.ASSIGNMENTS_UPDATED,
                fromStatus,
                nextStatus,
                currentUser,
                null,
                "staff=" + requestedBranchIdsByUserId.size()
                        + ", branchAssignments=" + branchLinkCount
        );

        return savedAssignments.stream()
                .map(assignment -> SparePartInventoryAssignmentResponse.from(
                        assignment,
                        branchAssignmentRepository.findActiveByAssignmentIdWithBranch(assignment.getId())
                ))
                .toList();
    }

    private void assertCanViewBranch(User currentUser, InventoryTask task, Long branchId) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (branchAssignmentRepository.existsActiveByTaskIdAndUserIdAndBranchId(task.getId(), currentUser.getId(), branchId)) {
            return;
        }
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);
    }

    private InventoryTask getSparePartTask(Long taskId) {
        InventoryTask task = inventoryTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task not found"));
        if (task.getInventoryDomain() != InventoryDomain.SPARE_PART) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task is not a spare part inventory task");
        }
        return task;
    }

    private InventoryTask getSparePartTaskForAssignmentUpdate(Long taskId) {
        InventoryTask task = inventoryTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task not found"));
        if (task.getInventoryDomain() != InventoryDomain.SPARE_PART) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task is not a spare part inventory task");
        }
        return task;
    }

    private SparePartInventoryBranch getTaskBranch(Long taskId, Long branchId) {
        return branchRepository.findById(branchId)
                .filter(branch -> branch.getInventoryTask() != null && branch.getInventoryTask().getId().equals(taskId))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Branch does not belong to this spare part inventory task"));
    }

    private void validateTaskCanBeAssigned(InventoryTask task) {
        if (task.getStatus() != InventoryTaskStatus.IMPORT_COMPLETED
                && task.getStatus() != InventoryTaskStatus.READY_FOR_ASSIGNMENT
                && task.getStatus() != InventoryTaskStatus.READY_TO_START
                && task.getStatus() != InventoryTaskStatus.IN_PROGRESS
                && task.getStatus() != InventoryTaskStatus.PAUSED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Spare-part inventory assignments cannot be changed in task status " + task.getStatus()
            );
        }
    }

    private InventoryTaskStatus resolveAssignmentTargetStatus(
            InventoryTask task,
            AssignInventoryTaskStaffBranchRequest request
    ) {
        if (request == null || request.getBranchAssignments() == null || request.getBranchAssignments().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "At least one spare part inventory staff assignment is required");
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
                throw new BusinessException(HttpStatus.BAD_REQUEST, "User " + user.getUsername() + " is not enabled for spare-part inventory");
            }
        }
        return usersById;
    }

    private List<InventoryTaskAssignment> synchronizeAssignments(
            InventoryTask task,
            User currentUser,
            Map<Long, List<Long>> requestedBranchIdsByUserId,
            Map<Long, User> staffById,
            Map<Long, SparePartInventoryBranch> taskBranchesById
    ) {
        Map<Long, InventoryTaskAssignment> existingByUserId = assignmentRepository
                .findByInventoryTaskId(task.getId())
                .stream()
                .collect(Collectors.toMap(assignment -> assignment.getUser().getId(), assignment -> assignment));

        for (Map.Entry<Long, InventoryTaskAssignment> entry : existingByUserId.entrySet()) {
            entry.getValue().setActive(requestedBranchIdsByUserId.containsKey(entry.getKey()));
        }
        assignmentRepository.saveAll(existingByUserId.values());

        List<InventoryTaskAssignment> activeAssignments = new ArrayList<>();
        for (Map.Entry<Long, List<Long>> requested : requestedBranchIdsByUserId.entrySet()) {
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
            synchronizeBranchAssignments(
                    assignment,
                    new LinkedHashSet<>(requested.getValue()),
                    taskBranchesById
            );
            activeAssignments.add(assignment);
        }
        return activeAssignments;
    }

    private void synchronizeBranchAssignments(
            InventoryTaskAssignment assignment,
            Set<Long> requestedBranchIds,
            Map<Long, SparePartInventoryBranch> taskBranchesById
    ) {
        Map<Long, SparePartInventoryBranchAssignment> existingByBranchId = branchAssignmentRepository
                .findByAssignmentId(assignment.getId())
                .stream()
                .collect(Collectors.toMap(value -> value.getBranch().getId(), value -> value));

        for (Map.Entry<Long, SparePartInventoryBranchAssignment> entry : existingByBranchId.entrySet()) {
            entry.getValue().setActive(requestedBranchIds.contains(entry.getKey()));
        }
        branchAssignmentRepository.saveAll(existingByBranchId.values());

        for (Long branchId : requestedBranchIds) {
            if (!existingByBranchId.containsKey(branchId)) {
                SparePartInventoryBranchAssignment branchAssignment = new SparePartInventoryBranchAssignment();
                branchAssignment.setAssignment(assignment);
                branchAssignment.setBranch(taskBranchesById.get(branchId));
                branchAssignment.setActive(true);
                branchAssignmentRepository.save(branchAssignment);
            }
        }
    }

    private Map<Long, List<Long>> normalizeBranchAssignments(List<StaffBranchAssignmentRequest> branchAssignments) {
        if (branchAssignments == null || branchAssignments.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "At least one spare part inventory staff assignment is required");
        }

        Map<Long, List<Long>> branchIdsByUserId = new LinkedHashMap<>();

        for (StaffBranchAssignmentRequest assignmentRequest : branchAssignments) {
            if (assignmentRequest == null || assignmentRequest.getUserId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "User id is required in branch assignment");
            }

            Long userId = assignmentRequest.getUserId();

            if (branchIdsByUserId.containsKey(userId)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Duplicate staff branch assignment is not allowed. userId=" + userId);
            }

            List<Long> branchIds = assignmentRequest.getBranchIds() == null
                    ? List.of()
                    : assignmentRequest.getBranchIds()
                    .stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            branchIdsByUserId.put(userId, branchIds);
        }

        return branchIdsByUserId;
    }

    private void validateEveryUserHasAtLeastOneBranch(Map<Long, List<Long>> requestedBranchIdsByUserId) {
        if (requestedBranchIdsByUserId == null || requestedBranchIdsByUserId.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "At least one spare part inventory staff assignment is required");
        }

        for (Map.Entry<Long, List<Long>> entry : requestedBranchIdsByUserId.entrySet()) {
            Long userId = entry.getKey();
            List<Long> branchIds = entry.getValue();

            if (branchIds == null || branchIds.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Each spare part inventory staff user must have at least one assigned branch. userId=" + userId);
            }
        }
    }

    private Map<Long, SparePartInventoryBranch> loadTaskBranchesById(InventoryTask task) {
        return branchRepository.findByInventoryTaskId(task.getId())
                .stream()
                .collect(Collectors.toMap(SparePartInventoryBranch::getId, branch -> branch));
    }

    private void validateRequestedBranchesBelongToTask(
            Map<Long, List<Long>> requestedBranchIdsByUserId,
            Map<Long, SparePartInventoryBranch> taskBranchesById
    ) {
        Set<Long> taskBranchIds = taskBranchesById.keySet();

        for (List<Long> branchIds : requestedBranchIdsByUserId.values()) {
            for (Long branchId : branchIds) {
                if (!taskBranchIds.contains(branchId)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "Branch does not belong to this spare part inventory task. branchId=" + branchId);
                }
            }
        }
    }
}
