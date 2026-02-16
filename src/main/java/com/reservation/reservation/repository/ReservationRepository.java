package com.reservation.reservation.repository;

import com.reservation.reservation.domain.Reservation;
import com.reservation.reservation.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByStatusAndExpiresAtBefore (
        ReservationStatus status, LocalDateTime time
    );
}