package com.pinetechs.orvix.ims.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import com.pinetechs.orvix.ims.user.enums.UserType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_user_type", columnList = "user_type"),
                @Index(name = "idx_users_enabled", columnList = "is_enabled"),
                @Index(name = "idx_users_deleted", columnList = "is_deleted")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @JsonIgnore
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @NotBlank(message = "First name is required")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Email(message = "Email is not valid")
    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "mobile", length = 30)
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 50)
    private UserType userType = UserType.INVENTORY_STAFF;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_channel", nullable = false, length = 20)
    private AccessChannel accessChannel = AccessChannel.MOBILE;

    /**
     * Companies that the user is allowed to work on.
     * SYSTEM_ADMIN: empty set, because he manages the platform globally.
     * COMPANY_ADMIN: one or more companies, reports only.
     * SUPERVISOR: one or more companies, task operations + reports according to inventoryDomains.
     * INVENTORY_STAFF: mobile user; task assignment will control the actual work, but company scope is still stored.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_companies",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_company", columnNames = {"user_id", "company_id"})
    )
    private Set<Company> companies = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_code", nullable = false, length = 100)
    private Set<PermissionCode> permissions = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_inventory_domains", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_domain", nullable = false, length = 50)
    private Set<InventoryDomain> inventoryDomains = new HashSet<>();

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User() {}

    public User(Long id) { this.id = id; }

    @PrePersist
    public void prePersist() {
        if (enabled == null) enabled = true;
        if (deleted == null) deleted = false;
        if (userType == null) userType = UserType.INVENTORY_STAFF;
        if (accessChannel == null) accessChannel = AccessChannel.MOBILE;
        if (username != null) username = username.trim().toLowerCase();
    }

    @PreUpdate
    public void preUpdate() {
        if (username != null) username = username.trim().toLowerCase();
    }

    public boolean hasPermission(PermissionCode permissionCode) {
        return permissions != null && permissions.contains(permissionCode);
    }

    public boolean hasInventoryDomain(InventoryDomain domain) {
        return inventoryDomains != null && inventoryDomains.contains(domain);
    }

    public boolean hasCompany(Long companyId) {
        if (companyId == null || companies == null) {
            return false;
        }
        for (Company company : companies) {
            if (company != null && companyId.equals(company.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isSystemAdmin() { return userType == UserType.SYSTEM_ADMIN; }
    public boolean isCompanyAdmin() { return userType == UserType.COMPANY_ADMIN; }
    public boolean isSupervisor() { return userType == UserType.SUPERVISOR; }
    public boolean isInventoryStaff() { return userType == UserType.INVENTORY_STAFF; }
    public boolean isPintechsStaff() { return userType == UserType.PINETECHS_SUPPORT_STAFF; }

    public String getFullName() {
        String fn = firstName == null ? "" : firstName.trim();
        String ln = lastName == null ? "" : lastName.trim();
        return (fn + " " + ln).trim();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getUserName() { return username; }
    public void setUserName(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
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
    public AccessChannel getAccessChannel() { return accessChannel; }
    public void setAccessChannel(AccessChannel accessChannel) { this.accessChannel = accessChannel; }
    public Set<Company> getCompanies() { return companies; }
    public void setCompanies(Set<Company> companies) { this.companies = companies; }
    public Set<PermissionCode> getPermissions() { return permissions; }
    public void setPermissions(Set<PermissionCode> permissions) { this.permissions = permissions; }
    public Set<InventoryDomain> getInventoryDomains() { return inventoryDomains; }
    public void setInventoryDomains(Set<InventoryDomain> inventoryDomains) { this.inventoryDomains = inventoryDomains; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
