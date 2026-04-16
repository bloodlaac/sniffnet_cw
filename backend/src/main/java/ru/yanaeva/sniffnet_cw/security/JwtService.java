package ru.yanaeva.sniffnet_cw.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Service;
import ru.yanaeva.sniffnet_cw.config.AppProperties;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final AppProperties appProperties;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String generateToken(AppUserPrincipal principal) {
        return generateAccessToken(principal);
    }

    public String generateAccessToken(AppUserPrincipal principal) {
        return generateToken(principal, ACCESS_TOKEN_TYPE, appProperties.getJwt().getExpiration());
    }

    public String generateRefreshToken(AppUserPrincipal principal) {
        return generateToken(principal, REFRESH_TOKEN_TYPE, appProperties.getJwt().getRefreshExpiration());
    }

    private String generateToken(AppUserPrincipal principal, String tokenType, java.time.Duration expirationDuration) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationDuration);
        return Jwts.builder()
            .subject(principal.getUsername())
            .claim("userId", principal.getId())
            .claim("role", principal.getRoleCode())
            .claim(TOKEN_TYPE_CLAIM, tokenType)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, AppUserPrincipal principal) {
        return isAccessTokenValid(token, principal);
    }

    public boolean isAccessTokenValid(String token, AppUserPrincipal principal) {
        return isTokenValid(token, principal, ACCESS_TOKEN_TYPE);
    }

    public boolean isRefreshTokenValid(String token, AppUserPrincipal principal) {
        return isTokenValid(token, principal, REFRESH_TOKEN_TYPE);
    }

    private boolean isTokenValid(String token, AppUserPrincipal principal, String expectedTokenType) {
        String username = extractUsername(token);
        Claims claims = parseClaims(token);
        return username.equals(principal.getUsername())
            && expectedTokenType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
            && claims.getExpiration().after(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
