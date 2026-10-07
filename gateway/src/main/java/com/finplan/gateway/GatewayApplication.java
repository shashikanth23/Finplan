package com.finplan.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.util.Optional;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    KeyResolver clientIpKeyResolver(@Value("${finplan.gateway.trusted-proxy-cidrs:}") String trustedProxyCidrs) {
        ClientIpResolver resolver = new ClientIpResolver(trustedProxyCidrs);
        return exchange -> {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            return Mono.just(Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(address -> resolver.resolve(address.getAddress(), forwarded)).orElse("unknown"));
        };
    }
}
