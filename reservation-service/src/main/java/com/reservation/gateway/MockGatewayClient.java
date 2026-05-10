package com.reservation.gateway;

import com.reservation.observability.ExecutionContext;
import com.reservation.observability.ExecutionContextHolder;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MockGatewayClient implements GatewayClient {

    private static final String URL = "http://localhost:8080/mock/process";

    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "mockGateway", fallbackMethod = "fallback")
    @Override
    public String process(String requestId, String resourceId, String resultType, int delayMs) {
        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("resourceId", resourceId);
        request.put("resultType", resultType);
        request.put("delayMs", delayMs);

        return restTemplate.postForObject(URL, request, String.class);
    }

    private String fallback(String requestId, String resourceId, String resultType, int delayMs, Exception e) {
        if (e instanceof CallNotPermittedException) {
            ExecutionContext ctx = ExecutionContextHolder.get();
            if (ctx != null) ctx.setCbBlocked(true);
            return "TIMEOUT";
        }
        if (e instanceof ResourceAccessException) {
            return "TIMEOUT";
        }
        return "FAIL";
    }
}
