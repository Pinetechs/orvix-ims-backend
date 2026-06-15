package com.pinetechs.orvix.ims.auth.controller;

import com.pinetechs.orvix.ims.auth.dto.LoginRequest;
import com.pinetechs.orvix.ims.auth.dto.LoginResponse;
import com.pinetechs.orvix.ims.security.JwtTokenService;
import com.pinetechs.orvix.ims.security.JwtUserDetails;
import com.pinetechs.orvix.ims.config.Config;
import com.pinetechs.orvix.ims.config.Property;
import com.pinetechs.orvix.ims.user.dto.UserResponse;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final Config config;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenService jwtTokenService,
                          Config config) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.config = config;
    }

    /**
     * Web portal login.
     * The token is stored as HttpOnly cookie exactly like the original project concept.
     */
    @PostMapping({"/login", "/web/login"})
    public LoginResponse webLogin(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        JwtUserDetails principal = authenticate(request);
        if (principal.getUser().getAccessChannel() != AccessChannel.WEB && principal.getUser().getAccessChannel() != AccessChannel.BOTH) {
            throw new IllegalArgumentException("User is not allowed to access the web portal");
        }


        String token = jwtTokenService.generateToken(principal.getUser(), principal.getUser().getAccessChannel().name());
        response.addHeader(HttpHeaders.SET_COOKIE, buildJwtCookie(token, config.getProperty(Property.COOKIE_MAX_AGE_SECONDS)).toString());

        // Keep token in body for debugging/admin tools, but the web app should depend on cookie.
        return new LoginResponse(token, UserResponse.from(principal.getUser()));
    }

    /**
     * Mobile app login.
     * The token is returned in body and must be sent as Authorization: Bearer <token>.
     */
    @PostMapping("/mobile/login")
    public LoginResponse mobileLogin(@Valid @RequestBody LoginRequest request) {
        JwtUserDetails principal = authenticate(request);
        if (principal.getUser().getAccessChannel() != AccessChannel.MOBILE) {
            throw new IllegalArgumentException("User is not allowed to access the mobile app");
        }

        String token = jwtTokenService.generateToken(principal.getUser(), AccessChannel.MOBILE.name());
        return new LoginResponse(token, UserResponse.from(principal.getUser()));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        JwtUserDetails principal = (JwtUserDetails) authentication.getPrincipal();
        return UserResponse.from(principal.getUser());
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        ResponseCookie logoutCookie = buildJwtCookie("", 0L);

        response.addHeader(HttpHeaders.SET_COOKIE, logoutCookie.toString());

       return ResponseEntity.ok(logoutCookie.toString());
    }

    private JwtUserDetails authenticate(LoginRequest request) throws BadCredentialsException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        return (JwtUserDetails) authentication.getPrincipal();
    }

    private ResponseCookie buildJwtCookie(String token, Long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("jwt", token == null ? "" : token)
                .httpOnly(true)
                .secure(config.getProperty(Property.COOKIE_SECURE))
                .path("/")
                .sameSite(config.getProperty(Property.COOKIE_SAME_SITE))
                .maxAge(maxAgeSeconds == null ? 0L : maxAgeSeconds);

        String domain = config.getProperty(Property.COOKIE_DOMAIN);
        if (domain != null && !domain.trim().isEmpty() && !"localhost".equalsIgnoreCase(domain.trim())) {
            builder.domain(domain.trim());
        }

        return builder.build();
    }
}
