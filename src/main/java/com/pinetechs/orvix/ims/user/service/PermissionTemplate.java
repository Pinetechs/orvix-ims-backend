package com.pinetechs.orvix.ims.user.service;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import com.pinetechs.orvix.ims.user.enums.UserType;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public final class PermissionTemplate {

    private PermissionTemplate() {}

    public static Set<PermissionCode> defaultPermissions(UserType userType,
                                                         Set<InventoryDomain> domains,
                                                         Set<PermissionCode> requested) {
        if (userType == UserType.SYSTEM_ADMIN) {
            return new HashSet<>(EnumSet.of(
                    PermissionCode.COMPANY_VIEW,
                    PermissionCode.COMPANY_CREATE,
                    PermissionCode.COMPANY_UPDATE,
                    PermissionCode.COMPANY_DISABLE,
                    PermissionCode.USER_VIEW,
                    PermissionCode.USER_CREATE,
                    PermissionCode.USER_UPDATE,
                    PermissionCode.USER_DISABLE,
                    PermissionCode.USER_RESET_PASSWORD
            ));
        }

        if (userType == UserType.COMPANY_ADMIN) {
            return companyAdminReportOnlyPermissions();
        }

        if (userType == UserType.INVENTORY_STAFF) {
            return new HashSet<>(EnumSet.of(
                    PermissionCode.MOBILE_TASK_LIST,
                    PermissionCode.MOBILE_SCAN_CREATE,
                    PermissionCode.MOBILE_QUANTITY_ENTRY,
                    PermissionCode.MOBILE_LOCATION_CHANGE
            ));
        }

        Set<PermissionCode> result = requested == null ? new HashSet<>() : new HashSet<>(requested);
        if (domains != null) {
            for (InventoryDomain domain : domains) {
                addSupervisorDomainPermissions(result, domain);
            }
        }
        return result;
    }

    public static Set<InventoryDomain> defaultDomains(UserType userType, Set<InventoryDomain> requested) {
        if (userType == UserType.SYSTEM_ADMIN) {
            return new HashSet<>();
        }
        if (userType == UserType.COMPANY_ADMIN) {
            return new HashSet<>(EnumSet.allOf(InventoryDomain.class));
        }
        return requested == null ? new HashSet<>() : new HashSet<>(requested);
    }

    private static Set<PermissionCode> companyAdminReportOnlyPermissions() {
        return new HashSet<>(EnumSet.of(
                PermissionCode.VEHICLE_REPORT_VIEW,
                PermissionCode.ASSET_REPORT_VIEW,
                PermissionCode.SPARE_PART_REPORT_VIEW
        ));
    }

    private static void addSupervisorDomainPermissions(Set<PermissionCode> permissions, InventoryDomain domain) {
        if (domain == InventoryDomain.VEHICLE) {
            permissions.add(PermissionCode.VEHICLE_TASK_VIEW);
            permissions.add(PermissionCode.VEHICLE_TASK_CREATE);
            permissions.add(PermissionCode.VEHICLE_TASK_UPDATE);
            permissions.add(PermissionCode.VEHICLE_TASK_ASSIGN_USERS);
            permissions.add(PermissionCode.VEHICLE_TASK_CLOSE);
            permissions.add(PermissionCode.VEHICLE_REPORT_VIEW);
        } else if (domain == InventoryDomain.ASSET) {
            permissions.add(PermissionCode.ASSET_TASK_VIEW);
            permissions.add(PermissionCode.ASSET_TASK_CREATE);
            permissions.add(PermissionCode.ASSET_TASK_UPDATE);
            permissions.add(PermissionCode.ASSET_TASK_ASSIGN_USERS);
            permissions.add(PermissionCode.ASSET_TASK_CLOSE);
            permissions.add(PermissionCode.ASSET_REPORT_VIEW);
        } else if (domain == InventoryDomain.SPARE_PART) {
            permissions.add(PermissionCode.SPARE_PART_TASK_VIEW);
            permissions.add(PermissionCode.SPARE_PART_TASK_CREATE);
            permissions.add(PermissionCode.SPARE_PART_TASK_UPDATE);
            permissions.add(PermissionCode.SPARE_PART_TASK_ASSIGN_USERS);
            permissions.add(PermissionCode.SPARE_PART_TASK_CLOSE);
            permissions.add(PermissionCode.SPARE_PART_REPORT_VIEW);
        }
    }
}
