package com.mtesazi.gatewayservice.security;

import com.mtesazi.gatewayservice.config.GatewayAuthProperties;
import io.jsonwebtoken.Claims;
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

    public boolean isValidAccessToken(String token) {
        Claims claims = parseClaims(token);
        String subject = claims.getSubject();
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        return subject != null
                && !subject.isBlank()
                && ACCESS_TOKEN_TYPE.equals(tokenType)
                && claims.getExpiration().after(new Date());
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
