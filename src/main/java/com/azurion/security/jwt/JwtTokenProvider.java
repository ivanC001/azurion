package com.azurion.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties properties;

    public String generateToken(String username, Long userId, String tenantId, List<String> roles,
                                List<String> permissions, List<String> modules) {
        Instant now = Instant.now();
        return generateToken(
                username,
                userId,
                tenantId,
                roles,
                permissions,
                modules,
                java.util.UUID.randomUUID().toString(),
                now.plus(properties.expiration())
        );
    }

    public String generateToken(String username, Long userId, String tenantId, List<String> roles,
                                List<String> permissions, List<String> modules, String sessionId,
                                Instant expiry) {
        Instant now = Instant.now();

        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(username)
                .claim("uid", userId.toString())
                .claim("userId", userId)
                .claim("tenant", tenantId)
                .claim("sid", sessionId)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("modules", modules)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Collection<? extends GrantedAuthority> authoritiesFrom(Claims claims) {
        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        if (claims.get("roles") instanceof List<?> roles) {
            roles.stream()
                    .map(String::valueOf)
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .forEach(authorities::add);
        }
        if (claims.get("permissions") instanceof List<?> permissions) {
            permissions.stream().map(String::valueOf).forEach(authorities::add);
        }
        return authorities.stream().map(SimpleGrantedAuthority::new).toList();
    }

    public List<String> modulesFrom(Claims claims) {
        if (claims.get("modules") instanceof List<?> modules) {
            return modules.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(String::toUpperCase)
                    .toList();
        }
        return List.of();
    }

    private SecretKey signingKey() {
        byte[] keyBytes;
        String secret = properties.getSecret();
        if (secret.matches("^[A-Za-z0-9+/=]+$") && secret.length() >= 44) {
            keyBytes = Decoders.BASE64.decode(secret);
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
