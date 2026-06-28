package com.pinetechs.orvix.ims.security;

import com.pinetechs.orvix.ims.auth.service.JwtUserDetailsService;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String JWT_COOKIE_NAME = "jwt";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final JwtUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, JwtUserDetailsService userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {



        TokenSource tokenSource = resolveToken(request);

        if (tokenSource == null || tokenSource.token == null || tokenSource.token.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String username;
        try {
            username = jwtTokenService.getUsernameFromToken(tokenSource.token);
            tokenSource.setChannel(jwtTokenService.getAccessChannelFromToken(tokenSource.token));
        } catch (IllegalArgumentException | ExpiredJwtException | SignatureException | MalformedJwtException | NullPointerException e) {

            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            JwtUserDetails userDetails = (JwtUserDetails) userDetailsService.loadUserByUsername(username);

            if (!isChannelAllowed(request, tokenSource, userDetails)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid access channel");
                return;
            }



            if (jwtTokenService.validateToken(tokenSource.token, userDetails)
                    && jwtTokenService.validateLastUpdate(tokenSource.token, userDetails.getUser())) {

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }


        }


        filterChain.doFilter(request, response);
    }

    private TokenSource resolveToken(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path != null && path.startsWith("/api/mobile/")) {
            return resolveBearerToken(request);
        }

        TokenSource cookieToken = resolveCookieToken(request);
        if (cookieToken != null) {
            return cookieToken;
        }

        return resolveBearerToken(request);
    }

    private TokenSource resolveCookieToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookie != null && JWT_COOKIE_NAME.equals(cookie.getName())) {

                String token = cookie.getValue();

                return new TokenSource(token);
            }
        }
        return null;
    }

    private TokenSource resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {

            String token = header.substring(BEARER_PREFIX.length());
            return new TokenSource(token);
        }

        return null;
    }

    private boolean isChannelAllowed(HttpServletRequest request, TokenSource tokenSource, JwtUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null || userDetails.getUser().getAccessChannel() == null) {
            return false;
        }

        String path = request.getRequestURI();
        AccessChannel userChannel = userDetails.getUser().getAccessChannel();



        if (tokenSource.channel == AccessChannel.BOTH && userChannel == AccessChannel.BOTH) {
            return true;
        }

        if (path != null && path.startsWith("/api/mobile/")) {
            return tokenSource.channel == AccessChannel.MOBILE && userChannel == AccessChannel.MOBILE;
        }

        return tokenSource.channel == AccessChannel.WEB && userChannel == AccessChannel.WEB;
    }

    private static class TokenSource {
        private final String token;
        private  AccessChannel channel;

        private TokenSource(String token) {
            this.token = token;
        }

        public void setChannel(AccessChannel channel) {
            this.channel = channel;
        }
    }
}
