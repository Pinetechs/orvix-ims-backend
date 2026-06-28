package com.pinetechs.orvix.ims.security;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessPolicyService {

    public void assertCanManageCompany(User user) {
        if (user != null && user.isSystemAdmin() && user.hasPermission(PermissionCode.COMPANY_CREATE)) {
            return;
        }
        throw new AccessDeniedException("Only system admin can manage companies");
    }


    public void assertCanViewUser(User user) {
       if (user == null ) {
           throw new AccessDeniedException("Authentication required");
       }

        for (PermissionCode permission : user.getPermissions()) {
            if (permission == PermissionCode.USER_VIEW) {
                return;
            }
        }

        throw new AccessDeniedException("User view permission is required");
    }




    public void   assertPermission(User user ,PermissionCode permission , String message){

            if (user == null ) {
                throw new BusinessException( HttpStatus.UNAUTHORIZED , "Authentication required");
            }

            for (PermissionCode p : user.getPermissions()) {
                if (p == permission) {
                    return;
                }


            }

            throw new BusinessException(HttpStatus.UNAUTHORIZED ,message);
    }




    public void assertCanViewInventoryTask(User user, Long companyId, InventoryDomain domain) {
        if (user == null){
            throw new BusinessException(HttpStatus.UNAUTHORIZED,"Authentication required");
        }

        PermissionCode permissionCode = taskViewPermission(domain);

        for (PermissionCode permission : user.getPermissions()) {
            if (permission == permissionCode) {
                // this for supervisor, if he has permission to create task, but he is not assigned to any company, then he should not be allowed to create task
                if (user.getCompanies().isEmpty()) {
                    return;
                }

                if (user.hasCompany(companyId)) {
                    return;
                } else {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "User is not allowed to view this task for this company");
                }
            }
        }

        throw new BusinessException(HttpStatus.BAD_REQUEST, "User is not allowed to view this task for this company");

    }


    public void assertCanImportExcel(User user, Long companyId, InventoryDomain domain) {
        if (user == null){
            throw new BusinessException(HttpStatus.UNAUTHORIZED,"Authentication required");
        }
        PermissionCode permissionCode = taskUpdatePermission(domain);
        for (PermissionCode permission : user.getPermissions()) {
            if (permission == permissionCode ) {
                // this for supervisor, if he has permission to create task, but he is not assigned to any company, then he should not be allowed to create task
                if (user.getCompanies().isEmpty()){
                    return;
                }


                if (user.hasCompany(companyId)) {
                    return;
                } else {
                    throw new BusinessException(HttpStatus.BAD_REQUEST,"User is not allowed to import excel for this company");
                }


            }

        }
        throw new BusinessException(HttpStatus.BAD_REQUEST,"User is not allowed to import excel for this company");


    }


    public void assertCanCreateTask(User user, Long companyId, InventoryDomain domain) {


        if (user == null){
            throw new BusinessException(HttpStatus.UNAUTHORIZED,"Authentication required");
        }

        PermissionCode permissionCode = taskCreatePermission(domain);




        for (PermissionCode permission : user.getPermissions())  {


            if (permission == permissionCode ) {

                // this for supervisor, if he has permission to create task, but he is not assigned to any company, then he should not be allowed to create task
                if (user.getCompanies().isEmpty()){
                    return;
                }

                if (user.hasCompany(companyId)) {
                    return;
                } else {
                    throw new BusinessException(HttpStatus.BAD_REQUEST,"User is not allowed to create this task for this company");
                }

            }

        }

        throw new BusinessException(HttpStatus.BAD_REQUEST,"User is not allowed to create this task");

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

    private PermissionCode reportPermission(InventoryDomain domain) {
        if (domain == InventoryDomain.VEHICLE) return PermissionCode.VEHICLE_REPORT_VIEW;
        if (domain == InventoryDomain.ASSET) return PermissionCode.ASSET_REPORT_VIEW;
        return PermissionCode.SPARE_PART_REPORT_VIEW;
    }



}
