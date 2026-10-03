package com.finplan.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.util.Optional;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    /** Rate-limit per client IP, honouring X-Forwarded-For when running behind an ingress. */
    @Bean
    KeyResolver clientIpKeyResolver() {
        return exchange -> {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) return Mono.just(forwarded.split(",")[0].trim());
            return Mono.just(Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(a -> a.getAddress().getHostAddress()).orElse("unknown"));
        };
    }
}
