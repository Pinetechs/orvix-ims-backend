package com.pinetechs.orvix.ims.user.controller;

import com.pinetechs.orvix.ims.auth.security.JwtUserDetails;
import com.pinetechs.orvix.ims.common.ApiResponse;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.user.dto.CreateUserRequest;
import com.pinetechs.orvix.ims.user.dto.ResetPasswordRequest;
import com.pinetechs.orvix.ims.user.dto.UpdateUserRequest;
import com.pinetechs.orvix.ims.user.dto.UserResponse;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import com.pinetechs.orvix.ims.user.enums.UserType;
import com.pinetechs.orvix.ims.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userManagementService.createUser(request, currentUser(authentication)));
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id,
                                   @Valid @RequestBody UpdateUserRequest request,
                                   Authentication authentication) {
        return userManagementService.updateUser(id, request, currentUser(authentication));
    }

    @GetMapping
    public Page<UserResponse> getUsers(@RequestParam(name = "page", defaultValue = "0") int page,
                                       @RequestParam(name = "size", defaultValue = "20") int size,
                                       @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
                                       @RequestParam(name = "sortOrder", defaultValue = "desc") String sortOrder,
                                       Authentication authentication) {
        Sort sort = "asc".equalsIgnoreCase(sortOrder) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return userManagementService.getUsers(pageable, currentUser(authentication));
    }

    @PutMapping("/{id}/disable")
    public ApiResponse disableUser(@PathVariable Long id, Authentication authentication) {
        userManagementService.disableUser(id, currentUser(authentication));
        return ApiResponse.ok("User disabled successfully");
    }

    @PutMapping("/{id}/reset-password")
    public ApiResponse resetPassword(@PathVariable Long id,
                                     @Valid @RequestBody ResetPasswordRequest request,
                                     Authentication authentication) {
        userManagementService.resetPassword(id, request.getNewPassword(), currentUser(authentication));
        return ApiResponse.ok("Password reset successfully");
    }

    @GetMapping("/user-types")
    public List<UserType> getUserTypes() { return Arrays.asList(UserType.values()); }

    @GetMapping("/permissions")
    public List<PermissionCode> getPermissions() { return Arrays.asList(PermissionCode.values()); }

    @GetMapping("/inventory-domains")
    public List<InventoryDomain> getInventoryDomains() { return Arrays.asList(InventoryDomain.values()); }

    private com.pinetechs.orvix.ims.user.entity.User currentUser(Authentication authentication) {
        return ((JwtUserDetails) authentication.getPrincipal()).getUser();
    }
}
