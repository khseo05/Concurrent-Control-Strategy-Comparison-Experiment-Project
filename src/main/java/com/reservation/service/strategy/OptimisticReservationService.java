package com.reservation.service.strategy;

import com.reservation.service.ReservationTxService;
import com.reservation.observability.ExecutionContext;
import com.reservation.observability.ExecutionContextHolder;
import com.reservation.domain.Concert;
import com.reservation.domain.Reservation;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class OptimisticReservationService implements ReservationStrategy {

    private final ReservationTxService txService;

    @Override
    public Reservation createPending(Long concertId) {
        ExecutionContext context = ExecutionContextHolder.get();

        int maxRetry = 5;
        int attempt = 0;

        while (attempt < maxRetry) {
            try {
                return txService.createPending(concertId);
            } catch (ObjectOptimisticLockingFailureException e) {
                attempt++;

                if (context != null) {
                    context.increaseConflict();
                    context.increaseRetry();
                }

                sleep(30);
            }
        }

        throw new ObjectOptimisticLockingFailureException(Concert.class, concertId);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}