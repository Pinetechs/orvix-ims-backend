package com.pinetechs.orvix.ims.auth.security;

import com.pinetechs.orvix.ims.config.Config;
import com.pinetechs.orvix.ims.config.Property;
import com.pinetechs.orvix.ims.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
public class JwtTokenService {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USER_TYPE = "userType";
    public static final String CLAIM_COMPANY_IDS = "companyIds";
    public static final String CLAIM_LAST_UPDATE = "lastUpdate";
    public static final String CLAIM_CHANNEL = "channel";

    private final Config config;

    public JwtTokenService(Config config) {
        this.config = config;
    }

    public String generateToken(User user, String channel) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId());
        claims.put(CLAIM_USER_TYPE, user.getUserType().name());
        claims.put(CLAIM_CHANNEL, channel);
        claims.put(CLAIM_LAST_UPDATE, getUserLastUpdateEpochMs(user));

        Set<Long> companyIds = new HashSet<>();
        if (user.getCompanies() != null) {
            user.getCompanies().forEach(company -> {
                if (company != null && company.getId() != null) {
                    companyIds.add(company.getId());
                }
            });
        }
        claims.put(CLAIM_COMPANY_IDS, companyIds);

        long expirationMs = config.getProperty(Property.JWT_TOKEN_VALIDITY_MS);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(SignatureAlgorithm.HS512, getSecret())
                .compact();
    }

    public String generateToken(User user) {
        return generateToken(user, user.getAccessChannel() == null ? "WEB" : user.getAccessChannel().name());
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Long getUserIdFromToken(String token) {
        Number number = getClaimFromToken(token, claims -> claims.get(CLAIM_USER_ID, Number.class));
        return number == null ? null : number.longValue();
    }

    public String getChannelFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get(CLAIM_CHANNEL, String.class));
    }

    public Long getLastUpdateFromToken(String token) {
        Number number = getClaimFromToken(token, claims -> claims.get(CLAIM_LAST_UPDATE, Number.class));
        return number == null ? null : number.longValue();
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser().setSigningKey(getSecret()).parseClaimsJws(token).getBody();
        return claimsResolver.apply(claims);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = getUsernameFromToken(token);
        return username.equals(userDetails.getUsername()) && !getExpirationDateFromToken(token).before(new Date());
    }

    public boolean validateLastUpdate(String token, User user) {
        Long tokenLastUpdate = getLastUpdateFromToken(token);
        if (tokenLastUpdate == null || user == null) {
            return false;
        }
        return tokenLastUpdate.equals(getUserLastUpdateEpochMs(user));
    }

    private long getUserLastUpdateEpochMs(User user) {
        LocalDateTime value = user.getUpdatedAt() != null ? user.getUpdatedAt() : user.getCreatedAt();
        if (value == null) {
            return 0L;
        }
        return value.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private String getSecret() {
        return config.getProperty(Property.JWT_SECRET);
    }
}
