package com.reservation.service.strategy;

import com.reservation.service.ReservationTxService;
import com.reservation.observability.ExecutionContext;
import com.reservation.observability.ExecutionContextHolder;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class OptimisticReservationService implements ReservationStrategy {

    private final ReservationTxService txService;

    @Override
    public void reserve(Long concertId) {

        int maxRetry = 10;
        int attempt = 0;

        while (attempt < maxRetry) {
            try {
                txService.createReservaton(concertId);
                return;
            } catch (ObjectOptimisticLockingFailureException e) {

                ExecutionContext context = ExecutionContextHolder.get();
                if (context != null) {
                    context.increaseRetry();
                    context.increaseConflict();
                }

                attempt++;
            }
        }

        throw new IllegalStateException("충돌 과다 발생");
    }
}