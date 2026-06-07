package com.pinetechs.orvix.ims.security;

import com.pinetechs.orvix.ims.inventory.enums.InventoryDomain;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AccessPolicyService {

    public void assertCanManageCompany(User user) {
        if (user != null && user.isSystemAdmin() && user.hasPermission(PermissionCode.COMPANY_CREATE)) {
            return;
        }
        throw new AccessDeniedException("Only system admin can manage companies");
    }

    public void assertCanCreateTask(User user, Long companyId, InventoryDomain domain) {
        if (user == null || !user.isSupervisor()) {
            throw new AccessDeniedException("Only supervisor can create inventory tasks");
        }
        if (!user.hasCompany(companyId)) {
            throw new AccessDeniedException("Supervisor is not allowed to work on this company");
        }
        if (!user.hasInventoryDomain(domain)) {
            throw new AccessDeniedException("Supervisor is not allowed to manage this inventory domain");
        }
        if (!user.hasPermission(taskCreatePermission(domain))) {
            throw new AccessDeniedException("Task create permission is required");
        }
    }

    public void assertCanViewReport(User user, Long companyId, InventoryDomain domain) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (user.isSystemAdmin()) {
            throw new AccessDeniedException("System admin does not access operational inventory reports by default");
        }
        if (!user.hasCompany(companyId)) {
            throw new AccessDeniedException("User is not allowed to view this company");
        }
        if (user.isCompanyAdmin()) {
            if (!user.hasPermission(reportPermission(domain))) {
                throw new AccessDeniedException("Report view permission is required");
            }
            return;
        }
        if (user.isSupervisor()) {
            if (!user.hasInventoryDomain(domain) || !user.hasPermission(reportPermission(domain))) {
                throw new AccessDeniedException("Supervisor is not allowed to view this report");
            }
            return;
        }
        throw new AccessDeniedException("Inventory staff cannot access web reports");
    }

    public void assertCanUseMobile(User user) {
        if (user != null && user.isInventoryStaff() && user.hasPermission(PermissionCode.MOBILE_SCAN_CREATE)) {
            return;
        }
        throw new AccessDeniedException("Only inventory staff can use the mobile inventory app");
    }

    private PermissionCode taskCreatePermission(InventoryDomain domain) {
        if (domain == InventoryDomain.VEHICLE) return PermissionCode.VEHICLE_TASK_CREATE;
        if (domain == InventoryDomain.ASSET) return PermissionCode.ASSET_TASK_CREATE;
        return PermissionCode.SPARE_PART_TASK_CREATE;
    }

    private PermissionCode reportPermission(InventoryDomain domain) {
        if (domain == InventoryDomain.VEHICLE) return PermissionCode.VEHICLE_REPORT_VIEW;
        if (domain == InventoryDomain.ASSET) return PermissionCode.ASSET_REPORT_VIEW;
        return PermissionCode.SPARE_PART_REPORT_VIEW;
    }
}
