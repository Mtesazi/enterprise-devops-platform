package com.mtesazi.gatewayservice.filter;

import com.mtesazi.gatewayservice.config.GatewayAuthProperties;
import io.jsonwebtoken.Claims;
import com.mtesazi.gatewayservice.security.JwtTokenValidator;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AuthPreparationFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";
    private static final Set<HttpMethod> WRITE_METHODS = Set.of(
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.PATCH,
            HttpMethod.DELETE
    );
    private static final List<String> ROLE_RESTRICTED_PATTERNS = List.of(
            "/api/v1/employees/**",
            "/api/employees/**",
            "/api/v1/departments/**"
    );

    private final GatewayAuthProperties authProperties;
    private final JwtTokenValidator jwtTokenValidator;

    public AuthPreparationFilter(GatewayAuthProperties authProperties, JwtTokenValidator jwtTokenValidator) {
        this.authProperties = authProperties;
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!authProperties.isEnabled() || isPublicPath(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }

        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)
                || authorizationHeader.substring(BEARER_PREFIX.length()).isBlank()) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtTokenValidator.validateAndGetAccessClaims(token);
            if (!hasRequiredRole(exchange, claims)) {
                return forbidden(exchange, "Insufficient permissions");
            }
        } catch (JwtException | IllegalArgumentException ex) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean isPublicPath(String requestPath) {
        List<String> publicPaths = authProperties.getPublicPaths();
        for (String pattern : publicPaths) {
            if (PATH_MATCHER.match(pattern, requestPath)) {
                return true;
            }
        }
        return "/login".equals(requestPath)
                || "/register".equals(requestPath)
                || "/refresh".equals(requestPath)
                || "/me".equals(requestPath)
                || "/api/auth/login".equals(requestPath)
                || "/api/auth/register".equals(requestPath)
                || "/api/auth/refresh".equals(requestPath);
    }

    private boolean hasRequiredRole(ServerWebExchange exchange, Claims claims) {
        String requestPath = exchange.getRequest().getPath().value();
        boolean restrictedPath = ROLE_RESTRICTED_PATTERNS.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, requestPath));
        if (!restrictedPath) {
            return true;
        }

        Set<String> roles = extractRoles(claims);
        HttpMethod method = exchange.getRequest().getMethod();
        if (method != null && WRITE_METHODS.contains(method)) {
            return roles.contains(ROLE_ADMIN);
        }
        return roles.contains(ROLE_ADMIN) || roles.contains(ROLE_USER);
    }

    private Set<String> extractRoles(Claims claims) {
        Object rolesClaim = claims.get(ROLES_CLAIM);
        if (!(rolesClaim instanceof Collection<?> collection)) {
            return Set.of();
        }
        Set<String> roles = new HashSet<>();
        for (Object role : collection) {
            if (role instanceof String value && !value.isBlank()) {
                roles.add(value);
            }
        }
        return roles;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return errorResponse(exchange, message);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return errorResponse(exchange, message);
    }

    private Mono<Void> errorResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(body)));
    }
}
