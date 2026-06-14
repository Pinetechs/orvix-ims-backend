package com.pinetechs.orvix.ims.user.dto;

import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import com.pinetechs.orvix.ims.user.enums.UserType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class UserResponse {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String mobile;
    private UserType userType;
    private AccessChannel accessChannel;
    private Set<Long> companyIds;
    private Set<String> companyNames;
    private Set<PermissionCode> permissions;
    private Set<InventoryDomain> inventoryDomains;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.username = user.getUsername();
        response.firstName = user.getFirstName();
        response.lastName = user.getLastName();
        response.fullName = user.getFullName();
        response.email = user.getEmail();
        response.mobile = user.getMobile();
        response.userType = user.getUserType();
        response.accessChannel = user.getAccessChannel();
        response.companyIds = new HashSet<>();
        response.companyNames = new HashSet<>();
        if (user.getCompanies() != null) {
            for (Company company : user.getCompanies()) {
                if (company != null) {
                    response.companyIds.add(company.getId());
                    response.companyNames.add(company.getName());
                }
            }
        }
        response.permissions = user.getPermissions();
        response.inventoryDomains = user.getInventoryDomains();
        response.enabled = user.getEnabled();
        response.createdAt = user.getCreatedAt();
        response.updatedAt = user.getUpdatedAt();
        return response;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public UserType getUserType() { return userType; }
    public AccessChannel getAccessChannel() { return accessChannel; }
    public Set<Long> getCompanyIds() { return companyIds; }
    public Set<String> getCompanyNames() { return companyNames; }
    public Set<PermissionCode> getPermissions() { return permissions; }
    public Set<InventoryDomain> getInventoryDomains() { return inventoryDomains; }
    public Boolean getEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
