package com.pinetechs.orvix.ims.user.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import com.pinetechs.orvix.ims.user.enums.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

public class UpdateUserRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String email;
    private String mobile;

    @NotNull
    private UserType userType;

    private Set<Long> companyIds = new HashSet<>();
    private Boolean enabled;
    private Set<InventoryDomain> inventoryDomains = new HashSet<>();
    private Set<PermissionCode> permissions = new HashSet<>();

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }
    public Set<Long> getCompanyIds() { return companyIds; }
    public void setCompanyIds(Set<Long> companyIds) { this.companyIds = companyIds; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Set<InventoryDomain> getInventoryDomains() { return inventoryDomains; }
    public void setInventoryDomains(Set<InventoryDomain> inventoryDomains) { this.inventoryDomains = inventoryDomains; }
    public Set<PermissionCode> getPermissions() { return permissions; }
    public void setPermissions(Set<PermissionCode> permissions) { this.permissions = permissions; }
}
