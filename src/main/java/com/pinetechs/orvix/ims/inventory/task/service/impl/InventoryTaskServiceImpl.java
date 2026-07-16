package com.pinetechs.orvix.ims.inventory.task.service.impl;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.company.repository.CompanyRepository;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.dto.CreateInventoryTaskRequest;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartLocationProgressMode;
import com.pinetechs.orvix.ims.inventory.task.dto.*;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationRepository;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryLocationAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryLocationRepository;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryBranchAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryBranchRepository;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskService;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskPurgeService;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import com.pinetechs.orvix.ims.user.enums.UserType;
import com.pinetechs.orvix.ims.user.repository.UserRepository;
import com.pinetechs.orvix.ims.user.dto.UserResponse;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryTaskServiceImpl implements InventoryTaskService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final AccessPolicyService accessPolicyService;
    private final InventoryTaskAssignmentRepository assignmentRepository;
    private final VehicleInventoryLocationRepository vehicleLocationRepository;
    private final VehicleInventoryLocationAssignmentRepository locationAssignmentRepository;
    private final InventoryTaskPurgeService purgeService;
    private final AssetInventoryLocationRepository assetLocationRepository;
    private final AssetInventoryLocationAssignmentRepository assetLocationAssignmentRepository;
    private final SparePartInventoryBranchRepository spareBranchRepository;
    private final SparePartInventoryBranchAssignmentRepository spareBranchAssignmentRepository;


    public InventoryTaskServiceImpl(
            InventoryTaskRepository inventoryTaskRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            AccessPolicyService accessPolicyService,
            InventoryTaskAssignmentRepository assignmentRepository,
            VehicleInventoryLocationRepository vehicleLocationRepository,
            VehicleInventoryLocationAssignmentRepository locationAssignmentRepository,
            AssetInventoryLocationRepository assetLocationRepository,
            AssetInventoryLocationAssignmentRepository assetLocationAssignmentRepository,
            SparePartInventoryBranchRepository spareBranchRepository,
            SparePartInventoryBranchAssignmentRepository spareBranchAssignmentRepository,
            InventoryTaskPurgeService purgeService) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.accessPolicyService = accessPolicyService;
        this.assignmentRepository = assignmentRepository;
        this.vehicleLocationRepository = vehicleLocationRepository;
        this.locationAssignmentRepository = locationAssignmentRepository;
        this.assetLocationRepository = assetLocationRepository;
        this.assetLocationAssignmentRepository = assetLocationAssignmentRepository;
        this.spareBranchRepository = spareBranchRepository;
        this.spareBranchAssignmentRepository = spareBranchAssignmentRepository;
        this.purgeService = purgeService;
    }

    @Override
    public TaskResponse createTask(CreateInventoryTaskRequest createInventoryTaskRequest, User currentUser) {
        InventoryDomain inventoryDomain =    InventoryDomain.valueOf(createInventoryTaskRequest.getInventoryDomain().toUpperCase());
        Company company = companyRepository.findById(createInventoryTaskRequest.getCompanyId()).orElseThrow(() -> new RuntimeException("Company not found"));
        accessPolicyService.assertCanCreateTask(currentUser, company.getId(), inventoryDomain);
        InventoryTask task = new InventoryTask();
        task.setTaskNumber(generateTaskNumber());
        task.setCompany(company);
        task.setCreatedBy(currentUser);
        task.setInventoryDomain(inventoryDomain);
        task.setStatus(InventoryTaskStatus.CREATED);
        task.setTaskName(createInventoryTaskRequest.getTaskName());
        task.setDescription(createInventoryTaskRequest.getDescription());
        task.setScanImageRequired(!Boolean.FALSE.equals(createInventoryTaskRequest.getScanImageRequired()));
        SparePartLocationProgressMode progressMode = createInventoryTaskRequest.getSparePartLocationProgressMode();
        if (progressMode == null) progressMode = SparePartLocationProgressMode.BASIC;
        if (inventoryDomain != InventoryDomain.SPARE_PART
                && progressMode != SparePartLocationProgressMode.BASIC) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Detailed spare-part location progress is valid only for spare-part tasks");
        }
        task.setSparePartLocationProgressMode(progressMode);
        return TaskResponse.from( inventoryTaskRepository.save(task),purgeService.countScans(task));
    }





    @Override
    public TaskResponse startTask(Long taskId, User currentUser) {
        InventoryTask task = getTask(taskId);
        assertCanUpdateTask(currentUser, task);

        if (task.getStatus() != InventoryTaskStatus.IMPORT_COMPLETED
                && task.getStatus() != InventoryTaskStatus.READY_FOR_ASSIGNMENT
                && task.getStatus() != InventoryTaskStatus.READY_TO_START) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task cannot be started from status: " + task.getStatus());
        }

        validateTaskReadyToStart(task);

        task.setStatus(InventoryTaskStatus.IN_PROGRESS);

        if (task.getStartDate() == null) {
            task.setStartDate(LocalDate.now());
        }

        return TaskResponse.from( inventoryTaskRepository.save(task),purgeService.countScans(task));
    }

    @Override
    public TaskResponse markReadyToStart(Long taskId, User currentUser) {
        InventoryTask task = getTask(taskId);

        accessPolicyService.assertCanAssignInventoryTaskUsers(
                currentUser,
                task.getCompany().getId(),
                task.getInventoryDomain()
        );

        if (task.getStatus() == InventoryTaskStatus.READY_TO_START) {
            return TaskResponse.from(task,purgeService.countScans(task));
        }

        if (task.getStatus() != InventoryTaskStatus.IMPORT_COMPLETED
                && task.getStatus() != InventoryTaskStatus.READY_FOR_ASSIGNMENT
                && task.getStatus() != InventoryTaskStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task cannot be marked ready from status: " + task.getStatus());
        }

        validateTaskReadyToStart(task);

        task.setStatus(InventoryTaskStatus.READY_TO_START);
        return TaskResponse.from( inventoryTaskRepository.save(task),purgeService.countScans(task));
    }





    @Override
    public TaskResponse pauseTask(Long taskId, String pauseReason, User currentUser) {
        InventoryTask task = getTask(taskId);
        assertCanUpdateTask(currentUser, task);

        if (task.getStatus() != InventoryTaskStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT, "Only an IN_PROGRESS task can be paused");
        }

        task.setStatus(InventoryTaskStatus.PAUSED);
        task.setPausedAt(LocalDateTime.now());
        task.setPauseReason(requireReason(pauseReason, "Pause reason is required"));
        task.setPausedBy(currentUser);



        return TaskResponse.from( inventoryTaskRepository.save(task),purgeService.countScans(task));
    }

    @Override
    public TaskResponse resumeTask(Long taskId, User currentUser) {
        InventoryTask task = getTask(taskId);
        assertCanUpdateTask(currentUser, task);

        if (task.getStatus() != InventoryTaskStatus.PAUSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "Only a PAUSED task can be resumed");
        }

        task.setStatus(InventoryTaskStatus.IN_PROGRESS);

        return TaskResponse.from( inventoryTaskRepository.save(task),purgeService.countScans(task));
    }

    @Override
    public TaskResponse moveToReview(Long taskId, User currentUser) {
        InventoryTask task = getTask(taskId);
        assertCanUpdateTask(currentUser, task);

        if (task.getStatus() != InventoryTaskStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT, "Only an IN_PROGRESS task can move to review");
        }

        task.setStatus(InventoryTaskStatus.UNDER_REVIEW);

        return TaskResponse.from( inventoryTaskRepository.save(task),purgeService.countScans(task));
    }

    @Override
    public TaskResponse completeTask(Long taskId, User currentUser) {
        InventoryTask task = getTask(taskId);
        assertCanUpdateTask(currentUser, task);

        if (task.getStatus() != InventoryTaskStatus.UNDER_REVIEW) {
            throw new BusinessException(HttpStatus.CONFLICT, "Only an UNDER_REVIEW task can be completed");
        }

        task.setStatus(InventoryTaskStatus.COMPLETED);
        task.setClosedAt(LocalDateTime.now());

        return TaskResponse.from( inventoryTaskRepository.save(task),purgeService.countScans(task));
    }

    @Override
    public TaskResponse cancelTask(Long taskId, String cancelReason, User currentUser) {
        InventoryTask task = getTask(taskId);
        assertCanUpdateTask(currentUser, task);

        if (task.getStatus() == InventoryTaskStatus.COMPLETED
                || task.getStatus() == InventoryTaskStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT, "Closed task cannot be cancelled");
        }
        if (task.getStatus() == InventoryTaskStatus.IMPORT_PENDING
                || task.getStatus() == InventoryTaskStatus.IMPORT_IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Task cannot be cancelled while its import job is pending or running");
        }

        task.setStatus(InventoryTaskStatus.CANCELLED);
        task.setClosedAt(LocalDateTime.now());
        task.setCancelReason(requireReason(cancelReason, "Cancellation reason is required"));
        task.setCancelledBy(currentUser);

        return TaskResponse.from( inventoryTaskRepository.save(task),purgeService.countScans(task));
    }





    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId ,User currentUser) {
       // accessPolicyService.assertCanViewInventoryTask(currentUser, taskId);

        if (taskId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task ID is required");

        }

        InventoryTask task = inventoryTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Task not found"));

        accessPolicyService.assertCanViewInventoryTask(currentUser, task);

        return toTaskResponse(task);
    }

    @Override
    public TaskResponse updateSparePartLocationProgressMode(
            Long taskId,
            UpdateSparePartLocationProgressModeRequest request,
            User currentUser
    ) {
        if (request == null || request.getMode() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Location progress mode is required");
        }
        UpdateInventoryTaskScanSettingsRequest settings = new UpdateInventoryTaskScanSettingsRequest();
        settings.setSparePartLocationProgressMode(request.getMode());
        return updateScanSettings(taskId, settings, currentUser);
    }

    @Override
    public TaskResponse updateScanSettings(
            Long taskId,
            UpdateInventoryTaskScanSettingsRequest request,
            User currentUser
    ) {
        if (request == null || (request.getScanImageRequired() == null
                && request.getSparePartLocationProgressMode() == null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "At least one scan setting is required");
        }

        InventoryTask task = getTask(taskId);
        assertCanUpdateTask(currentUser, task);
        assertTaskIsOpen(task, "Scan settings cannot be changed for a closed task");

        if (request.getScanImageRequired() != null) {
            task.setScanImageRequired(request.getScanImageRequired());
        }
        if (request.getSparePartLocationProgressMode() != null) {
            if (task.getInventoryDomain() != InventoryDomain.SPARE_PART) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "Location progress mode is valid only for spare-part tasks");
            }
            task.setSparePartLocationProgressMode(request.getSparePartLocationProgressMode());
        }

        return toTaskResponse(inventoryTaskRepository.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getEligibleStaff(
            Long taskId,
            String search,
            Pageable pageable,
            User currentUser
    ) {
        InventoryTask task = getTask(taskId);
        accessPolicyService.assertCanAssignInventoryTaskUsers(
                currentUser, task.getCompany().getId(), task.getInventoryDomain());
        return userRepository.findEligibleInventoryStaff(
                task.getCompany().getId(),
                task.getInventoryDomain(),
                normalizeSearch(search),
                UserType.INVENTORY_STAFF,
                pageable
        ).map(UserResponse::from);
    }

    @Override
    public void deleteTask(Long taskId, User currentUser) {
        if (taskId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task ID is required");
        }
        InventoryTask task = inventoryTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Task not found"));
        assertCanUpdateTask(currentUser, task);

        if (task.getStatus() == InventoryTaskStatus.COMPLETED
                || task.getStatus() == InventoryTaskStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT, "Closed task cannot be deleted");
        }
        if (task.getStatus() == InventoryTaskStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Pause the task before deleting it");
        }

        long scanCount = purgeService.countScans(task);
        if (scanCount >= InventoryTaskPurgeService.HARD_DELETE_SCAN_LIMIT) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Task has 10 or more scans and must be cancelled with a reason instead of deleted");
        }
        purgeService.purge(task);
    }





    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(Pageable pageable, User currentUser, String search, Long companyId, InventoryTaskStatus status, String inventoryDomains) {
        Set<InventoryDomain> allowedDomains = resolveAllowedDomainsForTaskView(currentUser, inventoryDomains);

        Set<Long> allowedCompanyIds = resolveAllowedCompanyIdsForTaskView(currentUser, companyId);

        String normalizedSearch = normalizeSearch(search);

        Specification<InventoryTask> specification = buildTaskSpecification(
                normalizedSearch,
                allowedCompanyIds,
                allowedDomains,
                status
        );

        return inventoryTaskRepository.findAll(specification, pageable).map(this::toTaskResponse);
    }


    private Set<InventoryDomain> resolveAllowedDomainsForTaskView(User user, String requestedDomains) {
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Set<InventoryDomain> allowed = EnumSet.noneOf(InventoryDomain.class);

        if (user.hasPermission(PermissionCode.VEHICLE_TASK_VIEW)) {
            allowed.add(InventoryDomain.VEHICLE);
        }

        if (user.hasPermission(PermissionCode.ASSET_TASK_VIEW)) {
            allowed.add(InventoryDomain.ASSET);
        }

        if (user.hasPermission(PermissionCode.SPARE_PART_TASK_VIEW)) {
            allowed.add(InventoryDomain.SPARE_PART);
        }

        if (allowed.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "User does not have permission to view inventory tasks"
            );
        }

        if (user.getInventoryDomains() != null && !user.getInventoryDomains().isEmpty()) {
            allowed.retainAll(user.getInventoryDomains());
        }

        Set<InventoryDomain> requested = parseInventoryDomains(requestedDomains);

        if (requested != null && !requested.isEmpty()) {
            allowed.retainAll(requested);
        }

        if (allowed.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "User is not allowed to view tasks for the requested inventory domain"
            );
        }

        return allowed;
    }


    private Set<Long> resolveAllowedCompanyIdsForTaskView(User user, Long requestedCompanyId) {
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        if (requestedCompanyId != null && !companyRepository.existsById(requestedCompanyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Company not found");
        }

        boolean globalCompanyAccess = user.isSystemAdmin() || user.isPintechsStaff();

        if (globalCompanyAccess) {
            if (requestedCompanyId == null) {
                return null;
            }

            return Set.of(requestedCompanyId);
        }

        if (user.getCompanies() == null || user.getCompanies().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Current user is not linked to any company"
            );
        }

        Set<Long> userCompanyIds = user.getCompanies()
                .stream()
                .map(Company::getId)
                .collect(Collectors.toSet());

        if (requestedCompanyId == null) {
            return userCompanyIds;
        }

        if (!userCompanyIds.contains(requestedCompanyId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "User is not allowed to view tasks for this company"
            );
        }

        return Set.of(requestedCompanyId);
    }


    private Set<InventoryDomain> parseInventoryDomains(String domains) {
        if (domains == null || domains.isBlank()) {
            return null;
        }

        return Arrays.stream(domains.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return InventoryDomain.valueOf(value.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ex) {
                        throw new BusinessException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid inventory domain: " + value
                        );
                    }
                })
                .collect(Collectors.toSet());
    }

    private Specification<InventoryTask> buildTaskSpecification(String search, Set<Long> companyIds, Set<InventoryDomain> domains, InventoryTaskStatus status) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = new HashSet<>();

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("company", JoinType.LEFT);
                root.fetch("createdBy", JoinType.LEFT);
                root.fetch("pausedBy", JoinType.LEFT);
                root.fetch("cancelledBy", JoinType.LEFT);
                query.distinct(true);
            }

            predicates.add(root.get("inventoryDomain").in(domains));

            if (companyIds != null && !companyIds.isEmpty()) {
                predicates.add(root.get("company").get("id").in(companyIds));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null) {
                Join<Object, Object> companyJoin = root.join("company", JoinType.LEFT);
                Join<Object, Object> createdByJoin = root.join("createdBy", JoinType.LEFT);

                String like = "%" + search.toUpperCase(Locale.ROOT) + "%";

                predicates.add(cb.or(
                        cb.like(cb.upper(root.get("taskNumber")), like),
                        cb.like(cb.upper(root.get("taskName")), like),
                        cb.like(cb.upper(root.get("description")), like),
                        cb.like(cb.upper(companyJoin.get("name")), like),
                        cb.like(cb.upper(companyJoin.get("code")), like),
                        cb.like(cb.upper(createdByJoin.get("username")), like),
                        cb.like(cb.upper(createdByJoin.get("firstName")), like),
                        cb.like(cb.upper(createdByJoin.get("lastName")), like)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }




    private void validateTaskReadyToStart(InventoryTask task) {
        long assignmentCount = assignmentRepository.findActiveByInventoryTaskIdWithUser(task.getId()).size();

        if (assignmentCount == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Assign at least one inventory staff member before starting the task");
        }

        if (task.getInventoryDomain() == InventoryDomain.VEHICLE) {
            long locationCount = vehicleLocationRepository.countByInventoryTaskId(task.getId());

            if (locationCount == 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Vehicle task has no imported locations. Import the vehicle Excel file first");
            }

            long assignedLocationCount = locationAssignmentRepository.countDistinctActiveLocationsByTaskId(task.getId());

            if (assignedLocationCount < locationCount) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "All vehicle locations must be assigned before the task can be ready to start"
                );
            }
        } else if (task.getInventoryDomain() == InventoryDomain.ASSET) {
            long locationCount = assetLocationRepository.countByInventoryTaskId(task.getId());
            if (locationCount == 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "Asset task has no imported locations. Import the asset Excel file first");
            }
            long assignedLocationCount = assetLocationAssignmentRepository
                    .countDistinctActiveLocationsByTaskId(task.getId());
            if (assignedLocationCount < locationCount) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "All asset locations must be assigned before the task can be ready to start");
            }
        } else if (task.getInventoryDomain() == InventoryDomain.SPARE_PART) {
            long branchCount = spareBranchRepository.countByInventoryTaskId(task.getId());
            if (branchCount == 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "Spare-part task has no imported branches. Import the spare-part Excel file first");
            }
            long assignedBranchCount = spareBranchAssignmentRepository
                    .countDistinctActiveBranchesByTaskId(task.getId());
            if (assignedBranchCount < branchCount) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "All spare-part branches must be assigned before the task can be ready to start");
            }
        }
    }

    private InventoryTask getTask(Long taskId) {
        return inventoryTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Inventory task not found"));
    }

    private TaskResponse toTaskResponse(InventoryTask task) {
        return TaskResponse.from(task, purgeService.countScans(task));
    }

    private void assertCanUpdateTask(User currentUser, InventoryTask task) {
        accessPolicyService.assertCanUpdateTask(
                currentUser, task.getCompany().getId(), task.getInventoryDomain());
    }

    private void assertTaskIsOpen(InventoryTask task, String message) {
        if (task.getStatus() == InventoryTaskStatus.COMPLETED
                || task.getStatus() == InventoryTaskStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT, message);
        }
    }

    private String requireReason(String reason, String message) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, message);
        }
        String normalized = reason.trim();
        if (normalized.length() > 500) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Reason must not exceed 500 characters");
        }
        return normalized;
    }
    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        return search.trim();
    }

    private String generateTaskNumber() {
        return "INV-" + System.currentTimeMillis();
    }
}
