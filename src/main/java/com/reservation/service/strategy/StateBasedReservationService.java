package com.reservation.service.strategy;

import com.reservation.domain.Reservation;
import com.reservation.service.ReservationTxService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StateBasedReservationService implements ReservationStrategy {

    private final ReservationTxService txService;

    @Override
    public Reservation createPending(Long concertId) {
        return txService.createPending(concertId);
    }
}