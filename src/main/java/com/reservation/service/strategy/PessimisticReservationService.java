package com.reservation.service.strategy;

import com.reservation.service.ReservationTxService;
import com.reservation.observability.ExecutionContext;
import com.reservation.observability.ExecutionContextHolder;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class PessimisticReservationService implements ReservationStrategy {

    private final ReservationTxService txService;

    @Override
    public void reserve(Long concertId) {
        txService.createReservaton(concertId);
    }
}
