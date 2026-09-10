package com.mtesazi.gatewayservice.security;

import com.mtesazi.gatewayservice.config.GatewayAuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenValidator {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final GatewayAuthProperties authProperties;

    public JwtTokenValidator(GatewayAuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public Claims validateAndGetAccessClaims(String token) {
        Claims claims = parseClaims(token);
        String subject = claims.getSubject();
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (subject == null || subject.isBlank()) {
            throw new JwtException("Token subject is missing");
        }
        if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
            throw new JwtException("Token is not an access token");
        }
        if (claims.getExpiration().before(new Date())) {
            throw new JwtException("Token has expired");
        }
        return claims;
    }

    public boolean isValidAccessToken(String token) {
        try {
            validateAndGetAccessClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(authProperties.getJwtSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
