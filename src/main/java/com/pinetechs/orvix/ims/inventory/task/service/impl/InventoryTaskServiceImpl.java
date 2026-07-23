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
import com.pinetechs.orvix.ims.inventory.task.enums.InventoryTaskActivityType;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationRepository;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryLocationAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryLocationRepository;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryBranchAssignmentRepository;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryBranchRepository;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskService;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskActivityService;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskPurgeService;
import com.pinetechs.orvix.ims.inventory.tracking.provider.InventoryTrackingProviderRegistry;
import com.pinetechs.orvix.ims.inventory.review.service.ReviewCenterService;
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
    private final InventoryTaskActivityService taskActivityService;
    private final InventoryTrackingProviderRegistry trackingProviderRegistry;
    private final ReviewCenterService reviewCenterService;


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
            InventoryTaskActivityService taskActivityService,
            InventoryTrackingProviderRegistry trackingProviderRegistry,
            ReviewCenterService reviewCenterService,
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
        this.taskActivityService = taskActivityService;
        this.trackingProviderRegistry = trackingProviderRegistry;
        this.reviewCenterService = reviewCenterService;
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
        InventoryTask savedTask = inventoryTaskRepository.save(task);
        taskActivityService.record(
                savedTask,
                InventoryTaskActivityType.TASK_CREATED,
                null,
                InventoryTaskStatus.CREATED,
                currentUser,
                null,
                "Inventory task created"
        );
        return toTaskResponse(savedTask, currentUser);
    }





    @Override
    public TaskResponse startTask(Long taskId, User currentUser) {
        InventoryTask task = getTaskForUpdate(taskId);
        assertCanUpdateTask(currentUser, task);

        if (task.getStatus() != InventoryTaskStatus.IMPORT_COMPLETED
                && task.getStatus() != InventoryTaskStatus.READY_FOR_ASSIGNMENT
                && task.getStatus() != InventoryTaskStatus.READY_TO_START) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task cannot be started from status: " + task.getStatus());
        }

        validateTaskReadyToStart(task);

        InventoryTaskStatus fromStatus = task.getStatus();
        LocalDateTime startedAt = LocalDateTime.now();
        task.setStatus(InventoryTaskStatus.IN_PROGRESS);

        if (task.getStartDate() == null) {
            task.setStartDate(startedAt.toLocalDate());
        }
        if (task.getStartedAt() == null) {
            task.setStartedAt(startedAt);
        }

        InventoryTask savedTask = inventoryTaskRepository.save(task);
        taskActivityService.record(
                savedTask,
                InventoryTaskActivityType.TASK_STARTED,
                fromStatus,
                InventoryTaskStatus.IN_PROGRESS,
                currentUser,
                null,
                "Task started manually"
        );
        return toTaskResponse(savedTask, currentUser);
    }

    @Override
    public TaskResponse markReadyToStart(Long taskId, User currentUser) {
        InventoryTask task = getTaskForUpdate(taskId);

        accessPolicyService.assertCanAssignInventoryTaskUsers(
                currentUser,
                task.getCompany().getId(),
                task.getInventoryDomain()
        );

        if (task.getStatus() == InventoryTaskStatus.READY_TO_START) {
            return toTaskResponse(task, currentUser);
        }

        if (task.getStatus() != InventoryTaskStatus.IMPORT_COMPLETED
                && task.getStatus() != InventoryTaskStatus.READY_FOR_ASSIGNMENT
                && task.getStatus() != InventoryTaskStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task cannot be marked ready from status: " + task.getStatus());
        }

        validateTaskReadyToStart(task);

        InventoryTaskStatus fromStatus = task.getStatus();
        task.setStatus(InventoryTaskStatus.READY_TO_START);
        InventoryTask savedTask = inventoryTaskRepository.save(task);
        taskActivityService.record(
                savedTask,
                InventoryTaskActivityType.MARKED_READY,
                fromStatus,
                InventoryTaskStatus.READY_TO_START,
                currentUser,
                null,
                "Task marked ready to start"
        );
        return toTaskResponse(savedTask, currentUser);
    }





    @Override
    public TaskResponse pauseTask(Long taskId, String pauseReason, User currentUser) {
        InventoryTask task = getTaskForUpdate(taskId);
        assertCanUpdateTask(currentUser, task);

        if (task.getStatus() != InventoryTaskStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT, "Only an IN_PROGRESS task can be paused");
        }

        String normalizedReason = requireReason(pauseReason, "Pause reason is required");
        InventoryTaskStatus fromStatus = task.getStatus();
        task.setStatus(InventoryTaskStatus.PAUSED);
        task.setPausedAt(LocalDateTime.now());
        task.setPauseReason(normalizedReason);
        task.setPausedBy(currentUser);

        InventoryTask savedTask = inventoryTaskRepository.save(task);
        taskActivityService.record(
                savedTask,
                InventoryTaskActivityType.TASK_PAUSED,
                fromStatus,
                InventoryTaskStatus.PAUSED,
                currentUser,
                normalizedReason,
                null
        );
        return toTaskResponse(savedTask, currentUser);
    }

    @Override
    public TaskResponse resumeTask(Long taskId, User currentUser) {
        InventoryTask task = getTaskForUpdate(taskId);
        assertCanUpdateTask(currentUser, task);

        if (task.getStatus() != InventoryTaskStatus.PAUSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "Only a PAUSED task can be resumed");
        }

        InventoryTaskStatus fromStatus = task.getStatus();
        task.setStatus(InventoryTaskStatus.IN_PROGRESS);
        task.setPausedAt(null);
        task.setPauseReason(null);
        task.setPausedBy(null);

        InventoryTask savedTask = inventoryTaskRepository.save(task);
        taskActivityService.record(
                savedTask,
                InventoryTaskActivityType.TASK_RESUMED,
                fromStatus,
                InventoryTaskStatus.IN_PROGRESS,
                currentUser,
                null,
                null
        );
        return toTaskResponse(savedTask, currentUser);
    }

    @Override
    public TaskResponse moveToReview(Long taskId, User currentUser) {
        InventoryTask task = getTaskForUpdate(taskId);
        assertCanUpdateTask(currentUser, task);

        if (task.getStatus() != InventoryTaskStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT, "Only an IN_PROGRESS task can move to review");
        }

        InventoryTaskStatus fromStatus = task.getStatus();
        task.setStatus(InventoryTaskStatus.UNDER_REVIEW);
        task.setReviewStartedAt(LocalDateTime.now());

        InventoryTask savedTask = inventoryTaskRepository.save(task);
        reviewCenterService.synchronize(savedTask.getId(), currentUser);
        taskActivityService.record(
                savedTask,
                InventoryTaskActivityType.SUBMITTED_FOR_REVIEW,
                fromStatus,
                InventoryTaskStatus.UNDER_REVIEW,
                currentUser,
                null,
                null
        );
        return toTaskResponse(savedTask, currentUser);
    }

    @Override
    public TaskResponse returnToProgress(Long taskId, String reason, User currentUser) {
        InventoryTask task = getTaskForUpdate(taskId);
        assertCanUpdateTask(currentUser, task);

        if (task.getStatus() != InventoryTaskStatus.UNDER_REVIEW) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Only an UNDER_REVIEW task can be returned to progress");
        }
        if (reviewCenterService.countActiveRechecks(taskId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Cancel or finish active recheck requests before returning the task to progress");
        }

        String normalizedReason = requireReason(reason, "Return reason is required");
        InventoryTaskStatus fromStatus = task.getStatus();
        task.setStatus(InventoryTaskStatus.IN_PROGRESS);
        task.setReviewStartedAt(null);

        InventoryTask savedTask = inventoryTaskRepository.save(task);
        taskActivityService.record(
                savedTask,
                InventoryTaskActivityType.RETURNED_TO_PROGRESS,
                fromStatus,
                InventoryTaskStatus.IN_PROGRESS,
                currentUser,
                normalizedReason,
                null
        );
        return toTaskResponse(savedTask, currentUser);
    }

    @Override
    public TaskResponse completeTask(Long taskId, User currentUser) {
        InventoryTask task = getTaskForUpdate(taskId);
        accessPolicyService.assertCanCloseTask(
                currentUser, task.getCompany().getId(), task.getInventoryDomain());

        if (task.getStatus() != InventoryTaskStatus.UNDER_REVIEW) {
            throw new BusinessException(HttpStatus.CONFLICT, "Only an UNDER_REVIEW task can be completed");
        }
        reviewCenterService.synchronize(taskId, currentUser);
        long blockingIssues = reviewCenterService.countBlockingOpenIssues(taskId);
        if (blockingIssues > 0) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Task has " + blockingIssues + " unresolved blocking review issue(s)");
        }
        if (reviewCenterService.countActiveRechecks(taskId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Task has active recheck requests");
        }

        InventoryTaskStatus fromStatus = task.getStatus();
        task.setStatus(InventoryTaskStatus.COMPLETED);
        task.setClosedAt(LocalDateTime.now());

        InventoryTask savedTask = inventoryTaskRepository.save(task);
        taskActivityService.record(
                savedTask,
                InventoryTaskActivityType.TASK_COMPLETED,
                fromStatus,
                InventoryTaskStatus.COMPLETED,
                currentUser,
                null,
                null
        );
        return toTaskResponse(savedTask, currentUser);
    }

    @Override
    public TaskResponse cancelTask(Long taskId, String cancelReason, User currentUser) {
        InventoryTask task = getTaskForUpdate(taskId);
        accessPolicyService.assertCanCloseTask(
                currentUser, task.getCompany().getId(), task.getInventoryDomain());

        if (task.getStatus() == InventoryTaskStatus.COMPLETED
                || task.getStatus() == InventoryTaskStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT, "Closed task cannot be cancelled");
        }
        if (task.getStatus() == InventoryTaskStatus.IMPORT_PENDING
                || task.getStatus() == InventoryTaskStatus.IMPORT_IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Task cannot be cancelled while its import job is pending or running");
        }
        if (reviewCenterService.countActiveRechecks(taskId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Cancel active recheck requests before cancelling the task");
        }

        String normalizedReason = requireReason(cancelReason, "Cancellation reason is required");
        InventoryTaskStatus fromStatus = task.getStatus();
        task.setStatus(InventoryTaskStatus.CANCELLED);
        task.setClosedAt(LocalDateTime.now());
        task.setCancelReason(normalizedReason);
        task.setCancelledBy(currentUser);

        InventoryTask savedTask = inventoryTaskRepository.save(task);
        taskActivityService.record(
                savedTask,
                InventoryTaskActivityType.TASK_CANCELLED,
                fromStatus,
                InventoryTaskStatus.CANCELLED,
                currentUser,
                normalizedReason,
                null
        );
        return toTaskResponse(savedTask, currentUser);
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

        return toTaskResponse(task, currentUser);
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

        InventoryTask task = getTaskForUpdate(taskId);
        assertCanUpdateTask(currentUser, task);
        assertTaskIsOpen(task, "Scan settings cannot be changed for a closed task");

        boolean previousScanImageRequired = task.isScanImageRequired();
        SparePartLocationProgressMode previousProgressMode = task.getSparePartLocationProgressMode();

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

        InventoryTask savedTask = inventoryTaskRepository.save(task);
        if (previousScanImageRequired != savedTask.isScanImageRequired()
                || previousProgressMode != savedTask.getSparePartLocationProgressMode()) {
            String details = "scanImageRequired: " + previousScanImageRequired
                    + " -> " + savedTask.isScanImageRequired()
                    + ", sparePartLocationProgressMode: " + previousProgressMode
                    + " -> " + savedTask.getSparePartLocationProgressMode();
            taskActivityService.record(
                    savedTask,
                    InventoryTaskActivityType.SCAN_SETTINGS_UPDATED,
                    savedTask.getStatus(),
                    savedTask.getStatus(),
                    currentUser,
                    null,
                    details
            );
        }
        return toTaskResponse(savedTask, currentUser);
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

        Page<InventoryTask> taskPage = inventoryTaskRepository.findAll(specification, pageable);
        Map<Long, Long> scanCounts = loadScanCounts(taskPage.getContent());
        return taskPage.map(task -> TaskResponse.from(
                task,
                scanCounts.getOrDefault(task.getId(), 0L),
                currentUser
        ));
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

    private InventoryTask getTaskForUpdate(Long taskId) {
        return inventoryTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Inventory task not found"));
    }

    private TaskResponse toTaskResponse(InventoryTask task, User viewer) {
        return TaskResponse.from(task, purgeService.countScans(task), viewer);
    }

    private Map<Long, Long> loadScanCounts(List<InventoryTask> tasks) {
        Map<Long, Long> counts = new HashMap<>();
        Map<InventoryDomain, List<Long>> taskIdsByDomain = tasks.stream()
                .collect(Collectors.groupingBy(
                        InventoryTask::getInventoryDomain,
                        () -> new EnumMap<>(InventoryDomain.class),
                        Collectors.mapping(InventoryTask::getId, Collectors.toList())
                ));
        taskIdsByDomain.forEach((domain, taskIds) ->
                trackingProviderRegistry.get(domain).eventMetrics(taskIds)
                        .forEach((taskId, metrics) -> counts.put(taskId, metrics.totalEvents())));
        return counts;
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
