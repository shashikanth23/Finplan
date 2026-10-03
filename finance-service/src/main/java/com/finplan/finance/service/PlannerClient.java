package com.finplan.finance.service;

import com.finplan.engine.FinancialInput;
import com.finplan.engine.FinancialPlanner;
import com.finplan.engine.FinancialSummary;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Calls planner-service with a timeout and a circuit breaker. If the planner is slow, down, or the breaker is open,
 * the dashboard degrades to the same engine library computed in-process instead of failing.
 */
@Component
public class PlannerClient {
    private static final Logger log = LoggerFactory.getLogger(PlannerClient.class);

    public record Result(FinancialSummary summary, String source) {}

    private final RestClient http;
    private final CircuitBreaker breaker;
    private final FinancialPlanner local = new FinancialPlanner();

    public PlannerClient(@Value("${finplan.planner.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.breaker = CircuitBreaker.of("planner", CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .build());
    }

    public Result summarize(FinancialInput input, String bearerToken) {
        try {
            FinancialSummary s = breaker.executeSupplier(() -> http.post()
                    .uri("/api/planner/summary")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .body(input)
                    .retrieve()
                    .body(FinancialSummary.class));
            if (s == null) throw new IllegalStateException("Empty planner response");
            return new Result(s, "planner-service");
        } catch (Exception e) {
            log.warn("planner-service unavailable ({}), using in-process engine", e.toString());
            return new Result(local.summarize(input), "local-fallback");
        }
    }
}
