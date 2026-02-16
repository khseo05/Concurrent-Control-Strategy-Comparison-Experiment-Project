package com.reservation.reservation.scheduler;

import com.reservation.reservation.domain.Concert;
import com.reservation.reservation.domain.Reservation;
import com.reservation.reservation.domain.ReservationStatus;
import com.reservation.reservation.repository.ConcertRepository;
import com.reservation.reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationScheduler {
    
    private final ReservationRepository reservationRepository;
    private final ConcertRepository concertRepository;

    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void cleanUpExpired() {
        List<Reservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, LocalDateTime.now());

        for (Reservation r : expired) {

            if (r.expireIfNecessary()) {
                Concert concert = concertRepository.findById(r.getConcertId()).orElseThrow();

                concert.increaseSeat();
            }
        }
    }
}