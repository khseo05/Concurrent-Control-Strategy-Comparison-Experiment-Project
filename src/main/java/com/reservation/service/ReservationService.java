package com.reservation.service;

import com.reservation.domain.Reservation;
import com.reservation.service.strategy.ReservationStrategy;
import com.reservation.observability.ExecutionContext;
import com.reservation.observability.ExecutionContextHolder;
import com.reservation.observability.MetricsCollector;
import com.reservation.observability.IdempotencyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationTxService txService;
    private final ReservationStrategy strategy;
    private final RestTemplate restTemplate;
    private final MetricsCollector metricsCollector;
    private final IdempotencyStore idempotencyStore;

    public void reserve(Long concertId, String resultType, int delayMs) {

        String requestId = java.util.UUID.randomUUID().toString();
        String key = requestId + ":" + concertId;

        String existing = idempotencyStore.get(key);
        if (existing != null) {
            return;
        }

        ExecutionContext context = new ExecutionContext();
        ExecutionContextHolder.set(context);

        context.setStartTime(System.currentTimeMillis());
        context.setStrategyType(strategy.getClass().getSimpleName());

        try {
            // 1. Tx1
            Reservation reservation = strategy.createPending(concertId);

            context.setProcessingStartTime(System.currentTimeMillis());

            // 2. 외부 호출
            String url = "http://localhost:8081/process";

            int maxRetry = 5;
            int attempt = 0;
            String result = "FAIL";

            while (attempt < maxRetry) {
                try {
                    Map<String, Object> request = new HashMap<>();
                    request.put("requestId", requestId);
                    request.put("resourceId", concertId.toString());
                    request.put("resultType", resultType);
                    request.put("delayMs", delayMs);

                    result = restTemplate.postForObject(url, request, String.class);
                    break;

                } catch(org.springframework.web.client.ResourceAccessException e) {
                    context.setMessage("TIMEOUT");
                    result = "TIMEOUT";
                    break;

                } catch (Exception e) {
                    attempt++;
                    context.increaseRetry();
                    sleep(100);
                }
            }

            // 3. Tx2
            txService.applyResult(reservation.getId(), result);

            context.setStatus(result);
        } finally {
            context.setEndTime(System.currentTimeMillis());
            metricsCollector.record(context);
            ExecutionContextHolder.clear();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }
}