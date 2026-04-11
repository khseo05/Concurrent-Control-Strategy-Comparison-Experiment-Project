package com.reservation.gateway;

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
    private static final int MAX_RETRY = 5;

    private final RestTemplate restTemplate;

    @Override
    public String process(String requestId, String resourceId, String resultType, int delayMs) {

        int attempt = 0;

        while (attempt < MAX_RETRY) {
            try {
                Map<String, Object> request = new HashMap<>();
                request.put("requestId", requestId);
                request.put("resourceId", resourceId);
                request.put("resultType", resultType);
                request.put("delayMs", delayMs);

                return restTemplate.postForObject(URL, request, String.class);

            } catch (ResourceAccessException e) {
                return "TIMEOUT";

            } catch (Exception e) {
                attempt++;
                sleep(100);
            }
        }

        return "FAIL";
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }
}
