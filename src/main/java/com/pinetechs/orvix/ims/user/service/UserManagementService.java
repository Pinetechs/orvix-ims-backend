package com.pinetechs.orvix.ims.user.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.company.repository.CompanyRepository;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.dto.CreateUserRequest;
import com.pinetechs.orvix.ims.user.dto.UpdateUserRequest;
import com.pinetechs.orvix.ims.user.dto.UserResponse;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import com.pinetechs.orvix.ims.user.enums.UserType;
import com.pinetechs.orvix.ims.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserManagementService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessPolicyService accessPolicyService ;

    public UserManagementService(UserRepository userRepository, CompanyRepository companyRepository, PasswordEncoder passwordEncoder, AccessPolicyService accessPolicyService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessPolicyService = accessPolicyService;
    }

    public UserResponse createUser(CreateUserRequest request, User currentUser) {
        accessPolicyService.assertPermission(currentUser, PermissionCode.USER_CREATE, "User create permission is required");

        if (currentUser.getUserType() != UserType.SYSTEM_ADMIN && request.getUserType() == UserType.PINETECHS_SUPPORT_STAFF) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only pinetechs support staff can create pinetechs support staff users");
        }

        String username = normalizeUsername(request.getUsername());
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }




       if ((currentUser.getUserType() != UserType.SYSTEM_ADMIN && currentUser.getUserType() != UserType.PINETECHS_SUPPORT_STAFF )&& request.getUserType() != UserType.SYSTEM_ADMIN) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only system admin can create system admin users");
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
        accessPolicyService.assertPermission(currentUser, PermissionCode.USER_UPDATE, "User update permission is required");

        User user = findUser(id);

        if (user.isPintechsStaff() && !currentUser.isPintechsStaff()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only pinetechs support staff can update pinetechs support staff users");
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
    public Page<UserResponse> getUsers(Pageable pageable, User currentUser , String search , UserType userType , AccessChannel accessChannel , String domains, Boolean status) {
        accessPolicyService.assertCanViewUser(currentUser);

        Set<InventoryDomain> inventoryDomains = PermissionTemplate.parseInventoryDomains(domains);




        if (currentUser.isSystemAdmin() || currentUser.isPintechsStaff()) {
            return userRepository.findByDeletedFalseAndSearchCriteria(search,userType,accessChannel,status,inventoryDomains,inventoryDomains == null?false:true,pageable).map(UserResponse::from);
        }

        if (currentUser.getCompanies() == null || currentUser.getCompanies().isEmpty()) {
            throw new BusinessException(HttpStatus.FORBIDDEN,"Current user is not linked to any company");
        }


        Set<Long> companyIds = extractCompanyIds(currentUser.getCompanies());
        return userRepository.findDistinctByDeletedFalseAndCompanies_IdIn(companyIds, pageable).map(UserResponse::from);
    }

    public void disableUser(Long id, User currentUser) {
        accessPolicyService.assertPermission(currentUser, PermissionCode.USER_DISABLE, "User disable permission is required");
        User user = findUser(id);
        if (user.isPintechsStaff() && !currentUser.isPintechsStaff()) {
            throw new BusinessException(HttpStatus.FORBIDDEN,"Only pinetechs support staff can disable PineTechs support staff");
        }
        assertUserIsInsideCurrentUserScope(user, currentUser);
        user.setEnabled(false);
        userRepository.save(user);
    }

    public void enableUser(Long id, User currentUser) {
        accessPolicyService.assertPermission(currentUser, PermissionCode.USER_DISABLE, "User enable permission is required");
        User user = findUser(id);

        if (user.isPintechsStaff() && !currentUser.isPintechsStaff()) {
            throw new BusinessException(HttpStatus.FORBIDDEN,"Only pinetechs support staff can enable PineTechs support staff");
        }
        assertUserIsInsideCurrentUserScope(user, currentUser);
        user.setEnabled(true);
        userRepository.save(user);
    }

    public void resetPassword(Long id, String newPassword, User currentUser) {
        accessPolicyService.assertPermission(currentUser, PermissionCode.USER_RESET_PASSWORD, "User reset password permission is required");
        User user = findUser(id);
        if (user.isPintechsStaff() && !currentUser.isPintechsStaff()) {
            throw new BusinessException(HttpStatus.FORBIDDEN,"Only system admin can disable PineTechs support staff");
        }
        assertUserIsInsideCurrentUserScope(user, currentUser);
        user.setPassword(passwordEncoder.encode(trimRequired(newPassword, "New password")));
        userRepository.save(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }





    private void applyCompaniesAndAccess(User user, Set<Long> requestedCompanyIds, User currentUser) {

        if (user.isSystemAdmin()) {
            user.getCompanies().clear();
            user.setAccessChannel(AccessChannel.WEB);
            return;
        }

        if (user.getUserType() == UserType.PINETECHS_SUPPORT_STAFF) {
            user.getCompanies().clear();
            user.setAccessChannel(AccessChannel.BOTH);
            return;
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
            Company company = companyRepository.findById(companyId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company not found: " + companyId));
            companies.add(company);
        }

        if (companies.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one valid company is required");
        }


       Set<Company> currentUserCompanies = currentUser.getCompanies();
        if (!currentUser.isSystemAdmin() && !currentUser.isPintechsStaff()) {

            for (Company company : companies) {
                boolean check = currentUserCompanies.stream().anyMatch(c -> c.getId().equals(company.getId()));
                if (!check) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current user does not have access to company: " + company.getName());
                }


            }
        }
        user.setCompanies(companies);
        user.setAccessChannel(user.isInventoryStaff() ? AccessChannel.APP : AccessChannel.WEB);
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
            if (user.getAccessChannel() != AccessChannel.APP) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory staff must use app channel");
            }
        }
        if (!user.isInventoryStaff() && user.getAccessChannel() == AccessChannel.APP) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only inventory staff can be an app-only user");
        }
    }

    private void assertUserIsInsideCurrentUserScope(User targetUser, User currentUser) {
        if (currentUser.isSystemAdmin() || currentUser.isPintechsStaff()) {
            return;
        }
        Set<Long> currentCompanyIds = extractCompanyIds(currentUser.getCompanies());
        for (Company company : targetUser.getCompanies()) {
            if (company != null && currentCompanyIds.contains(company.getId())) {
                return;
            }
        }
        throw new BusinessException(HttpStatus.FORBIDDEN,"Target user is outside your company scope");
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
