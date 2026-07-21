package com.pinetechs.orvix.ims.inventory.tracking.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.company.repository.CompanyRepository;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TrackingTaskScopeService {

    private final InventoryTaskRepository taskRepository;
    private final CompanyRepository companyRepository;
    private final AccessPolicyService accessPolicyService;

    public TrackingTaskScopeService(
            InventoryTaskRepository taskRepository,
            CompanyRepository companyRepository,
            AccessPolicyService accessPolicyService
    ) {
        this.taskRepository = taskRepository;
        this.companyRepository = companyRepository;
        this.accessPolicyService = accessPolicyService;
    }

    @Transactional(readOnly = true)
    public InventoryTask requireAccessibleTask(Long taskId, User currentUser) {
        if (taskId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task ID is required");
        }
        InventoryTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Inventory task not found"));
        accessPolicyService.assertCanViewInventoryTask(currentUser, task);
        return task;
    }

    @Transactional(readOnly = true)
    public Page<InventoryTask> findAccessibleTasks(
            User currentUser,
            String search,
            Long companyId,
            InventoryDomain domain,
            InventoryTaskStatus status,
            Pageable pageable
    ) {
        Scope scope = resolveScope(currentUser, companyId, domain);
        return taskRepository.findAll(specification(scope, search, status == null ? null : Set.of(status), null), pageable);
    }

    @Transactional(readOnly = true)
    public List<InventoryTask> findAccessibleTasks(
            User currentUser,
            Collection<InventoryTaskStatus> statuses,
            Sort sort
    ) {
        Scope scope = resolveScope(currentUser, null, null);
        Set<InventoryTaskStatus> statusSet = statuses == null || statuses.isEmpty()
                ? null
                : EnumSet.copyOf(statuses);
        return taskRepository.findAll(specification(scope, null, statusSet, null), sort);
    }

    @Transactional(readOnly = true)
    public long countAccessibleCompletedSince(User currentUser, LocalDateTime since) {
        Scope scope = resolveScope(currentUser, null, null);
        return taskRepository.count(specification(
                scope,
                null,
                Set.of(InventoryTaskStatus.COMPLETED),
                since
        ));
    }

    public boolean hasOperationalDashboardAccess(User user) {
        if (user == null || user.isSystemAdmin() || user.isInventoryStaff()) return false;
        try {
            resolveScope(user, null, null);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private Scope resolveScope(User user, Long requestedCompanyId, InventoryDomain requestedDomain) {
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Set<InventoryDomain> domains = accessPolicyService
                .getAccessibleDomainForInventoryTaskView(user, requestedDomain);

        if (requestedCompanyId != null && !companyRepository.existsById(requestedCompanyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Company not found");
        }

        if (user.isPintechsStaff() || user.isSystemAdmin()) {
            return new Scope(domains, requestedCompanyId == null ? null : Set.of(requestedCompanyId));
        }

        if (user.getCompanies() == null || user.getCompanies().isEmpty()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Current user is not linked to any company");
        }
        Set<Long> companyIds = user.getCompanies().stream().map(Company::getId).collect(Collectors.toSet());
        if (requestedCompanyId != null) {
            if (!companyIds.contains(requestedCompanyId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "User is not allowed to view tasks for this company");
            }
            companyIds = Set.of(requestedCompanyId);
        }
        return new Scope(domains, companyIds);
    }

    private Specification<InventoryTask> specification(
            Scope scope,
            String search,
            Set<InventoryTaskStatus> statuses,
            LocalDateTime closedSince
    ) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("company", JoinType.LEFT);
                query.distinct(true);
            }
            predicates.add(root.get("inventoryDomain").in(scope.domains()));
            if (scope.companyIds() != null && !scope.companyIds().isEmpty()) {
                predicates.add(root.get("company").get("id").in(scope.companyIds()));
            }
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (closedSince != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("closedAt"), closedSince));
            }
            if (normalizedSearch != null) {
                String like = "%" + normalizedSearch.toUpperCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.upper(root.get("taskNumber")), like),
                        cb.like(cb.upper(root.get("taskName")), like),
                        cb.like(cb.upper(root.get("description")), like),
                        cb.like(cb.upper(root.get("company").get("name")), like),
                        cb.like(cb.upper(root.get("company").get("code")), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private record Scope(Set<InventoryDomain> domains, Set<Long> companyIds) {}
}
