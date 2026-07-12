package com.pinetechs.orvix.ims.app.controller;

import com.pinetechs.orvix.ims.auth.dto.LoginRequest;
import com.pinetechs.orvix.ims.auth.dto.LoginResponse;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.security.JwtTokenService;
import com.pinetechs.orvix.ims.security.JwtUserDetails;
import com.pinetechs.orvix.ims.user.dto.UserResponse;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/v1/auth")
public class AppAuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final AccessPolicyService accessPolicyService;

    public AppAuthController(AuthenticationManager authenticationManager,
                             JwtTokenService jwtTokenService,
                             AccessPolicyService accessPolicyService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.accessPolicyService = accessPolicyService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = ((JwtUserDetails) authentication.getPrincipal()).getUser();
        if (user.getAccessChannel() != AccessChannel.APP && user.getAccessChannel() != AccessChannel.BOTH) {
            throw new org.springframework.security.access.AccessDeniedException("User is not allowed to access the app");
        }
        accessPolicyService.assertCanUseApp(user);

        String token = jwtTokenService.generateToken(user, AccessChannel.APP.name());
        return new LoginResponse(token, UserResponse.from(user));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        User user = ((JwtUserDetails) authentication.getPrincipal()).getUser();
        accessPolicyService.assertCanUseApp(user);
        return UserResponse.from(user);
    }
}
