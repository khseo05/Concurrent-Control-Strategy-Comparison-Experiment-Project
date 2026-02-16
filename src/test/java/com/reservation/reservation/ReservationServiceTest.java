package com.reservation.reservation.service;

import com.reservation.reservation.domain.Concert;
import com.reservation.reservation.domain.Reservation;
import com.reservation.reservation.domain.ReservationStatus;
import com.reservation.reservation.repository.ConcertRepository;
import com.reservation.reservation.repository.ReservationRepository;
import com.reservation.reservation.scheduler.ReservationScheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void reserve_success() {

        Concert concert = new Concert(10L);
        concertRepository.save(concert);

        reservationService.reserve(concert.getId());

        Reservation reservation = reservationRepository.findAll().get(0);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        Concert updated = concertRepository.findById(concert.getId()).orElseThrow();
        assertThat(updated.getSeat()).isEqualTo(9);
    }
}
