package com.pinetechs.orvix.ims.user.service;

import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.company.repository.CompanyRepository;
import com.pinetechs.orvix.ims.user.dto.CreateUserRequest;
import com.pinetechs.orvix.ims.user.dto.UpdateUserRequest;
import com.pinetechs.orvix.ims.user.dto.UserResponse;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import com.pinetechs.orvix.ims.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class UserManagementService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository userRepository,
                                 CompanyRepository companyRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(CreateUserRequest request, User currentUser) {
        assertCanManageUsers(currentUser);

        String username = normalizeUsername(request.getUsername());
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        user.setFirstName(trimRequired(request.getFirstName(), "First name"));
        user.setLastName(trimRequired(request.getLastName(), "Last name"));
        user.setEmail(trimToNull(request.getEmail()));
        user.setMobile(trimToNull(request.getMobile()));
        user.setUserType(request.getUserType());
        user.setEnabled(true);
        user.setDeleted(false);

        applyCompaniesAndAccess(user, request.getCompanyIds(), currentUser);
        user.setInventoryDomains(PermissionTemplate.defaultDomains(user.getUserType(), request.getInventoryDomains()));
        user.setPermissions(PermissionTemplate.defaultPermissions(user.getUserType(), user.getInventoryDomains(), request.getPermissions()));
        validateUserRules(user);

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request, User currentUser) {
        assertCanManageUsers(currentUser);

        User user = findUser(id);
        if (user.isSystemAdmin() && !currentUser.isSystemAdmin()) {
            throw new AccessDeniedException("Only system admin can update system admin users");
        }

        user.setFirstName(trimRequired(request.getFirstName(), "First name"));
        user.setLastName(trimRequired(request.getLastName(), "Last name"));
        user.setEmail(trimToNull(request.getEmail()));
        user.setMobile(trimToNull(request.getMobile()));
        user.setUserType(request.getUserType());
        user.setEnabled(request.getEnabled() == null || request.getEnabled());

        applyCompaniesAndAccess(user, request.getCompanyIds(), currentUser);
        user.setInventoryDomains(PermissionTemplate.defaultDomains(user.getUserType(), request.getInventoryDomains()));
        user.setPermissions(PermissionTemplate.defaultPermissions(user.getUserType(), user.getInventoryDomains(), request.getPermissions()));
        validateUserRules(user);

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Pageable pageable, User currentUser) {
        assertCanManageUsers(currentUser);
        if (currentUser.isSystemAdmin()) {
            return userRepository.findByDeletedFalse(pageable).map(UserResponse::from);
        }
        if (currentUser.getCompanies() == null || currentUser.getCompanies().isEmpty()) {
            throw new AccessDeniedException("Current user is not linked to any company");
        }
        Set<Long> companyIds = extractCompanyIds(currentUser.getCompanies());
        return userRepository.findDistinctByDeletedFalseAndCompanies_IdIn(companyIds, pageable).map(UserResponse::from);
    }

    public void disableUser(Long id, User currentUser) {
        assertCanManageUsers(currentUser);
        User user = findUser(id);
        if (user.isSystemAdmin() && !currentUser.isSystemAdmin()) {
            throw new AccessDeniedException("Only system admin can disable system admin users");
        }
        assertUserIsInsideCurrentUserScope(user, currentUser);
        user.setEnabled(false);
        userRepository.save(user);
    }

    public void resetPassword(Long id, String newPassword, User currentUser) {
        assertCanManageUsers(currentUser);
        User user = findUser(id);
        assertUserIsInsideCurrentUserScope(user, currentUser);
        user.setPassword(passwordEncoder.encode(trimRequired(newPassword, "New password")));
        userRepository.save(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void assertCanManageUsers(User currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (currentUser.isSystemAdmin() && currentUser.hasPermission(PermissionCode.USER_CREATE)) {
            return;
        }
        throw new AccessDeniedException("Only system admin can manage users");
    }

    private void applyCompaniesAndAccess(User user, Set<Long> requestedCompanyIds, User currentUser) {
        if (user.isSystemAdmin()) {
            user.getCompanies().clear();
            user.setAccessChannel(AccessChannel.WEB);
            return;
        }

        if (!currentUser.isSystemAdmin()) {
            throw new AccessDeniedException("Only system admin can assign users to companies");
        }

        Set<Long> companyIds = requestedCompanyIds == null ? new HashSet<>() : new HashSet<>(requestedCompanyIds);
        if (companyIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one company is required for non-system admin users");
        }

        Set<Company> companies = new HashSet<>();
        for (Long companyId : companyIds) {
            if (companyId == null) {
                continue;
            }
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company not found: " + companyId));
            companies.add(company);
        }

        if (companies.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one valid company is required");
        }

        user.setCompanies(companies);
        user.setAccessChannel(user.isInventoryStaff() ? AccessChannel.MOBILE : AccessChannel.WEB);
    }

    private void validateUserRules(User user) {
        if (user.getUserType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User type is required");
        }
        if (user.isSystemAdmin() && user.getCompanies() != null && !user.getCompanies().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "System admin must not be linked to companies");
        }
        if (!user.isSystemAdmin() && (user.getCompanies() == null || user.getCompanies().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Companies are required for non-system admin users");
        }
        if (user.isCompanyAdmin()) {
            if (user.getAccessChannel() != AccessChannel.WEB) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company admin must use web channel");
            }
            if (!user.hasPermission(PermissionCode.VEHICLE_REPORT_VIEW)
                    || !user.hasPermission(PermissionCode.ASSET_REPORT_VIEW)
                    || !user.hasPermission(PermissionCode.SPARE_PART_REPORT_VIEW)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company admin must have report-only permissions for all inventory domains");
            }
        }
        if (user.isSupervisor()) {
            if (user.getAccessChannel() != AccessChannel.WEB) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supervisor must use web channel");
            }
            if (user.getInventoryDomains() == null || user.getInventoryDomains().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory domains are required for supervisor users");
            }
        }
        if (user.isInventoryStaff()) {
            if (user.getAccessChannel() != AccessChannel.MOBILE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory staff must use mobile channel");
            }
        }
        if (!user.isInventoryStaff() && user.getAccessChannel() == AccessChannel.MOBILE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only inventory staff can be mobile-only user");
        }
    }

    private void assertUserIsInsideCurrentUserScope(User targetUser, User currentUser) {
        if (currentUser.isSystemAdmin()) {
            return;
        }
        Set<Long> currentCompanyIds = extractCompanyIds(currentUser.getCompanies());
        for (Company company : targetUser.getCompanies()) {
            if (company != null && currentCompanyIds.contains(company.getId())) {
                return;
            }
        }
        throw new AccessDeniedException("Target user is outside your company scope");
    }

    private Set<Long> extractCompanyIds(Set<Company> companies) {
        Set<Long> ids = new HashSet<>();
        if (companies != null) {
            for (Company company : companies) {
                if (company != null && company.getId() != null) {
                    ids.add(company.getId());
                }
            }
        }
        return ids;
    }

    private String normalizeUsername(String username) {
        return trimRequired(username, "Username").toLowerCase();
    }

    private String trimRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
