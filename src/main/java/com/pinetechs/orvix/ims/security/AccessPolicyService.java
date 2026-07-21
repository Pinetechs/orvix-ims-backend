package com.pinetechs.orvix.ims.security;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Service
public class AccessPolicyService {

    public void assertCanManageCompany(User user) {
        if (user != null && user.isSystemAdmin() && user.hasPermission(PermissionCode.COMPANY_CREATE)) {
            return;
        }
        throw new AccessDeniedException("Only system admin can manage companies");
    }

    public void assertCanViewUser(User user) {
        requireAuthenticated(user);
        if (user.hasPermission(PermissionCode.USER_VIEW)) {
            return;
        }
        throw new AccessDeniedException("User view permission is required");
    }

    public void assertPermission(User user, PermissionCode permission, String message) {
        requireAuthenticated(user);
        if (user.hasPermission(permission)) {
            return;
        }
        throw new BusinessException(HttpStatus.FORBIDDEN, message);
    }

    public Set<InventoryDomain> getAccessibleDomainForInventoryTaskView(
            User user,
            InventoryDomain requestedDomain
    ) {
        requireAuthenticated(user);

        Set<InventoryDomain> allowed = EnumSet.noneOf(InventoryDomain.class);
        for (InventoryDomain domain : InventoryDomain.values()) {
            if (user.hasPermission(taskViewPermission(domain))) {
                allowed.add(domain);
            }
        }

        if (user.getInventoryDomains() != null && !user.getInventoryDomains().isEmpty()) {
            allowed.retainAll(user.getInventoryDomains());
        }

        if (requestedDomain != null) {
            if (!allowed.contains(requestedDomain)) {
                throw forbidden("User is not allowed to view tasks for this inventory domain");
            }
            return EnumSet.of(requestedDomain);
        }

        if (allowed.isEmpty()) {
            throw forbidden("User does not have permission to view inventory tasks");
        }
        return allowed;
    }

    public Set<Company> getAccessibleCompaniesForInventoryTaskView(User user, Company company) {
        requireAuthenticated(user);
        requireAnyTaskViewPermission(user);

        if (hasGlobalCompanyScope(user)) {
            return company == null ? new HashSet<>() : new HashSet<>(Set.of(company));
        }

        requireCompanyAssignments(user);
        if (company == null) {
            return new HashSet<>(user.getCompanies());
        }
        assertCompanyScope(user, company.getId(), "User is not allowed to view tasks for this company");
        return new HashSet<>(Set.of(company));
    }

    public Set<Company> assertCanViewInventoryTask(User user, InventoryTask task) {
        requireAuthenticated(user);
        if (task == null || task.getInventoryDomain() == null || task.getCompany() == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Inventory task not found");
        }

        assertTaskViewPermission(user, task.getInventoryDomain());
        assertCompanyScope(user, task.getCompany().getId(),
                "User is not allowed to view this task for this company");
        return hasGlobalCompanyScope(user)
                ? new HashSet<>()
                : new HashSet<>(Set.of(task.getCompany()));
    }

    public Set<Company> assertCanViewInventoryTask(User user, InventoryDomain domain) {
        requireAuthenticated(user);
        assertTaskViewPermission(user, domain);
        if (hasGlobalCompanyScope(user)) {
            return new HashSet<>();
        }
        requireCompanyAssignments(user);
        return new HashSet<>(user.getCompanies());
    }

    public void assertCanViewInventoryTask(User user, Long companyId, InventoryDomain domain) {
        requireAuthenticated(user);
        assertTaskViewPermission(user, domain);
        assertCompanyScope(user, companyId, "User is not allowed to view this task for this company");
    }

    public void assertCanImportExcel(User user, Long companyId, InventoryDomain domain) {
        requireAuthenticated(user);
        assertPermission(user, taskUpdatePermission(domain), "Task update permission is required");
        assertCompanyScope(user, companyId, "User is not allowed to import Excel for this company");
    }

    public void assertCanUpdateTask(User user, Long companyId, InventoryDomain domain) {
        requireAuthenticated(user);
        assertPermission(user, taskUpdatePermission(domain), "Task update permission is required");
        assertCompanyScope(user, companyId, "User is not allowed to update this task for this company");
    }

    public void assertCanAssignInventoryTaskUsers(User user, Long companyId, InventoryDomain domain) {
        requireAuthenticated(user);
        assertPermission(user, taskAssignPermission(domain), "Task assignment permission is required");
        assertCompanyScope(user, companyId, "User is not allowed to assign users for this company");
    }

    public void assertCanCloseTask(User user, Long companyId, InventoryDomain domain) {
        requireAuthenticated(user);
        assertPermission(user, taskClosePermission(domain), "Task close permission is required");
        assertCompanyScope(user, companyId, "User is not allowed to close this task for this company");
    }

    public void assertCanCreateTask(User user, Long companyId, InventoryDomain domain) {
        requireAuthenticated(user);
        assertPermission(user, taskCreatePermission(domain), "Task create permission is required");
        assertCompanyScope(user, companyId, "User is not allowed to create a task for this company");
    }

    public void assertCanViewReport(User user, Long companyId, InventoryDomain domain) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (user.isSystemAdmin()) {
            throw new AccessDeniedException(
                    "System admin does not access operational inventory reports by default");
        }
        if (!user.hasPermission(reportPermission(domain))) {
            throw new AccessDeniedException("Report view permission is required");
        }
        if (user.isPintechsStaff()) {
            return;
        }
        if (!user.hasCompany(companyId)) {
            throw new AccessDeniedException("User is not allowed to view this company");
        }
        if (user.isCompanyAdmin()) {
            return;
        }
        if (user.isSupervisor() && user.hasInventoryDomain(domain)) {
            return;
        }
        throw new AccessDeniedException("User is not allowed to view this report");
    }

    public void assertCanUseApp(User user) {
        if (user != null && (user.isInventoryStaff() || user.hasPermission(PermissionCode.APP_TASK_LIST))) {
            return;
        }
        throw new AccessDeniedException("Only authorized inventory staff can use the inventory app");
    }

    public void assertCanCreateAppScan(User user) {
        assertAppPermission(user, PermissionCode.APP_SCAN_CREATE, "App scan permission is required");
    }

    public void assertCanEnterAppQuantity(User user) {
        assertAppPermission(user, PermissionCode.APP_QUANTITY_ENTRY, "App quantity entry permission is required");
    }

    public void assertCanCorrectAppScan(User user) {
        assertAppPermission(user, PermissionCode.APP_LOCATION_CHANGE, "App scan correction permission is required");
    }

    private void assertAppPermission(User user, PermissionCode permission, String message) {
        if (user != null && user.isInventoryStaff() && user.hasPermission(permission)) {
            return;
        }
        throw new AccessDeniedException(message);
    }

    private void assertTaskViewPermission(User user, InventoryDomain domain) {
        if (domain == null || !user.hasPermission(taskViewPermission(domain))) {
            throw forbidden("Task view permission is required for this inventory domain");
        }
        if (user.getInventoryDomains() != null
                && !user.getInventoryDomains().isEmpty()
                && !user.hasInventoryDomain(domain)) {
            throw forbidden("User is not allowed to view tasks for this inventory domain");
        }
    }

    private void requireAnyTaskViewPermission(User user) {
        for (InventoryDomain domain : InventoryDomain.values()) {
            if (user.hasPermission(taskViewPermission(domain))) {
                return;
            }
        }
        throw forbidden("User does not have permission to view inventory tasks");
    }

    private void assertCompanyScope(User user, Long companyId, String message) {
        if (companyId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Company ID is required");
        }
        if (hasGlobalCompanyScope(user)) {
            return;
        }
        requireCompanyAssignments(user);
        if (!user.hasCompany(companyId)) {
            throw forbidden(message);
        }
    }

    private void requireCompanyAssignments(User user) {
        if (user.getCompanies() == null || user.getCompanies().isEmpty()) {
            throw forbidden("Current user is not linked to any company");
        }
    }

    private boolean hasGlobalCompanyScope(User user) {
        return user.isPintechsStaff() || user.isSystemAdmin();
    }

    private void requireAuthenticated(User user) {
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, message);
    }

    private PermissionCode taskCreatePermission(InventoryDomain domain) {
        if (domain == InventoryDomain.VEHICLE) return PermissionCode.VEHICLE_TASK_CREATE;
        if (domain == InventoryDomain.ASSET) return PermissionCode.ASSET_TASK_CREATE;
        return PermissionCode.SPARE_PART_TASK_CREATE;
    }

    private PermissionCode taskViewPermission(InventoryDomain domain) {
        if (domain == InventoryDomain.VEHICLE) return PermissionCode.VEHICLE_TASK_VIEW;
        if (domain == InventoryDomain.ASSET) return PermissionCode.ASSET_TASK_VIEW;
        return PermissionCode.SPARE_PART_TASK_VIEW;
    }

    private PermissionCode taskUpdatePermission(InventoryDomain domain) {
        if (domain == InventoryDomain.VEHICLE) return PermissionCode.VEHICLE_TASK_UPDATE;
        if (domain == InventoryDomain.ASSET) return PermissionCode.ASSET_TASK_UPDATE;
        return PermissionCode.SPARE_PART_TASK_UPDATE;
    }

    private PermissionCode taskAssignPermission(InventoryDomain domain) {
        if (domain == InventoryDomain.VEHICLE) return PermissionCode.VEHICLE_TASK_ASSIGN_USERS;
        if (domain == InventoryDomain.ASSET) return PermissionCode.ASSET_TASK_ASSIGN_USERS;
        return PermissionCode.SPARE_PART_TASK_ASSIGN_USERS;
    }

    private PermissionCode taskClosePermission(InventoryDomain domain) {
        if (domain == InventoryDomain.VEHICLE) return PermissionCode.VEHICLE_TASK_CLOSE;
        if (domain == InventoryDomain.ASSET) return PermissionCode.ASSET_TASK_CLOSE;
        return PermissionCode.SPARE_PART_TASK_CLOSE;
    }

    private PermissionCode reportPermission(InventoryDomain domain) {
        if (domain == InventoryDomain.VEHICLE) return PermissionCode.VEHICLE_REPORT_VIEW;
        if (domain == InventoryDomain.ASSET) return PermissionCode.ASSET_REPORT_VIEW;
        return PermissionCode.SPARE_PART_REPORT_VIEW;
    }
}
