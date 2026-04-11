package com.reservation.service;

import com.reservation.domain.Reservation;
import com.reservation.gateway.GatewayClient;
import com.reservation.service.strategy.ReservationStrategy;
import com.reservation.observability.ExecutionContext;
import com.reservation.observability.ExecutionContextHolder;
import com.reservation.observability.MetricsCollector;
import com.reservation.observability.IdempotencyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationTxService txService;
    private final ReservationStrategy strategy;
    private final GatewayClient gatewayClient;
    private final MetricsCollector metricsCollector;
    private final IdempotencyStore idempotencyStore;

    public void reserve(Long concertId, String resultType, int delayMs) {
        reserve(concertId, resultType, delayMs, strategy);
    }

    public void reserve(Long concertId, String resultType, int delayMs, ReservationStrategy strategyOverride) {
        reserve(UUID.randomUUID().toString(), concertId, resultType, delayMs, strategyOverride);
    }

    public void reserve(String requestId, Long concertId, String resultType, int delayMs, ReservationStrategy strategyOverride) {

        String key = requestId + ":" + concertId;

        if (!idempotencyStore.saveIfAbsent(key, "IN_PROGRESS")) {
            metricsCollector.recordIdempotencyHit();
            return;
        }

        ExecutionContext context = new ExecutionContext();
        ExecutionContextHolder.set(context);

        context.setStartTime(System.currentTimeMillis());
        context.setStrategyType(strategyOverride.getClass().getSimpleName());

        try {
            // 1. Tx1
            Reservation reservation = strategyOverride.createPending(concertId);

            context.setProcessingStartTime(System.currentTimeMillis());

            // 2. 외부 호출
            String result = gatewayClient.process(requestId, concertId.toString(), resultType, delayMs);

            if ("TIMEOUT".equals(result)) {
                context.setMessage("TIMEOUT");
            }

            // 3. Tx2
            txService.applyResult(reservation.getId(), result);

            idempotencyStore.saveIfAbsent(key, result);
            context.setStatus(result);
        } finally {
            context.setEndTime(System.currentTimeMillis());
            metricsCollector.record(context);
            ExecutionContextHolder.clear();
        }
    }
}
