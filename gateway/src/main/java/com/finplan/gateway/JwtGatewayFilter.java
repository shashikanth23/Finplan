package com.finplan.gateway;

import com.finplan.security.JwtTokens;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rejects unauthenticated calls at the edge so bad traffic never reaches the services.
 * Services still validate the token themselves (defence in depth), because the gateway is not the only way in
 * inside a cluster.
 */
@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private final String mode;
    private final String secret;
    private final String publicKey;

    public JwtGatewayFilter(@Value("${finplan.jwt.mode:HS256}") String mode,
                            @Value("${finplan.jwt.secret:}") String secret,
                            @Value("${finplan.jwt.public-key:}") String publicKey,
                            @Value("${spring.profiles.active:}") String activeProfiles) {
        JwtTokens.validateVerificationConfiguration(mode, secret, publicKey, activeProfiles);
        this.mode = mode;
        this.secret = secret;
        this.publicKey = publicKey;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (request.getMethod() == HttpMethod.OPTIONS || isPublic(path)) return chain.filter(exchange);

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) return reject(exchange);
        try {
            JwtTokens.verify(header.substring(7), mode, secret, publicKey);
        } catch (JwtException | IllegalArgumentException e) {
            return reject(exchange);
        }
        return chain.filter(exchange);
    }

    private static boolean isPublic(String path) {
        return path.equals("/api/auth/login") || path.equals("/api/auth/register") || path.startsWith("/actuator");
    }

    private static Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() { return -1; }
}
