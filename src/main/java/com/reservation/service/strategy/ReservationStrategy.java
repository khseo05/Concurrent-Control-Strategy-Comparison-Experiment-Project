package com.reservation.service.strategy;

import com.reservation.domain.Reservation;

public interface ReservationStrategy {
    Reservation createPending(Long concertId);
}