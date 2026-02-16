package com.reservation.reservation.service;

import com.reservation.reservation.domain.Concert;
import com.reservation.reservation.domain.Reservation;
import com.reservation.reservation.domain.ReservationStatus;
import com.reservation.reservation.repository.ConcertRepository;
import com.reservation.reservation.repository.ReservationRepository;
import com.reservation.reservation.scheduler.ReservationScheduler;
import com.reservation.reservation.payment.PermanentPaymentException;
import com.reservation.reservation.payment.TemporaryPaymentException;
import com.reservation.reservation.payment.PaymentService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationTxService txService;

    @MockBean
    private PaymentService paymentService;

    @BeforeEach
    void clean() {
        reservationRepository.deleteAll();
        concertRepository.deleteAll();
    }

    @Test
    void reserve_success() {

        Concert concert = new Concert(10);
        concertRepository.save(concert);

        // 결제 성공 강제
        doNothing().when(paymentService).callPayment();

        reservationService.reserve(concert.getId());

        Reservation reservation = reservationRepository.findAll().get(0);

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);

        Concert updated =
                concertRepository.findById(concert.getId()).orElseThrow();

        assertThat(updated.getRemainingSeats())
                .isEqualTo(9);
    }


    @Test
    void reserve_fail_should_cancel_and_restore_seat() {

        Concert concert = new Concert(10);
        concertRepository.save(concert);

        doThrow(new PermanentPaymentException())
                .when(paymentService)
                .callPayment();

        reservationService.reserve(concert.getId());

        Reservation reservation = reservationRepository.findAll().get(0);

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);

        Concert updated =
                concertRepository.findById(concert.getId()).orElseThrow();

        assertThat(updated.getRemainingSeats())
                .isEqualTo(10);
    }


    @Test
    void expire_and_confirm_race_condition_test() throws Exception {

        Concert concert = new Concert(1);
        concertRepository.save(concert);

        // 결제 지연 시뮬레이션
        doAnswer(invocation -> {
            Thread.sleep(500);
            return null;
        }).when(paymentService).callPayment();

        Thread t = new Thread(() ->
                reservationService.reserve(concert.getId()));
        t.start();

        // reservation 생성 대기
        Reservation reservation = null;
        while (reservation == null) {
            if (!reservationRepository.findAll().isEmpty()) {
                reservation = reservationRepository.findAll().get(0);
            } else {
                Thread.sleep(10);
            }
        }

        reservation.forceExpireNow();
        reservationRepository.save(reservation);

        txService.expire(reservation.getId());

        t.join();

        Reservation updated =
                reservationRepository.findById(reservation.getId()).orElseThrow();

        assertThat(updated.getStatus())
                .isEqualTo(ReservationStatus.EXPIRED);

        Concert finalConcert =
                concertRepository.findById(concert.getId()).orElseThrow();

        assertThat(finalConcert.getRemainingSeats())
                .isEqualTo(1);
    }
}