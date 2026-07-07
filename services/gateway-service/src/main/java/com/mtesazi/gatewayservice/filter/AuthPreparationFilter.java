package com.mtesazi.gatewayservice.filter;

import com.mtesazi.gatewayservice.config.GatewayAuthProperties;
import com.mtesazi.gatewayservice.security.JwtTokenValidator;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthPreparationFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String BEARER_PREFIX = "Bearer ";

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
            if (!jwtTokenValidator.isValidAccessToken(token)) {
                return unauthorized(exchange, "Invalid or expired token");
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
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(body)));
    }
}
