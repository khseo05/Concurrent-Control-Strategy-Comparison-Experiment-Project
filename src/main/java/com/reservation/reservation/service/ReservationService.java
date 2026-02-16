package com.reservation.reservation.service;

import com.reservation.reservation.domain.*;
import com.reservation.reservation.repository.*;
import com.reservation.reservation.payment.TemporaryPaymentException;
import com.reservation.reservation.payment.PermanentPaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ConcertRepository concertRepository;
    private final ReservationRepository reservationRepository;

    public void reserve(Long concertId) {

        Reservation reservation = createReservaton(concertId);

        int maxRetry = 3;
        int attempt = 0;
        
        while (attempt < maxRetry) {
            try {
                callPaymentApi();
                confirm(reservation.getId());
                System.out.println("결제 성공");
                return;
            } catch (TemporaryPaymentException e) {
                attempt++;
                System.out.println("일시 장애 - 재시도: " + attempt);
                sleep(100);
            } catch (PermanentPaymentException e) {
                System.out.println("결제 거절 - 즉시 취소");
                cancel(reservation.getId());
                return;
            }
        }

        System.out.println("재시도 초과 - 취소 처리");
        cancel(reservation.getId());
    }

    @Transactional
    public Reservation createReservaton(Long concertId) {
        Concert concert = concertRepository.findById(concertId).orElseThrow();
        concert.decreaseSeat();

        Reservation reservation = new Reservation(concertId);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void confirm(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        reservation.confirm();
    }

    @Transactional
    public void cancel(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();

        Concert concert = concertRepository.findById(reservation.getConcertId()).orElseThrow();
        concert.increaseSeat();

        reservation.cancel();
    }

    private boolean callPaymentApi() {
        double rand = Math.random();

        if (rand < 0.3) {
            throw new TemporaryPaymentException();
        } else if (rand < 0.6) {
            throw new PermanentPaymentException();
        }
    }

    private void sleep(long millis)  {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }
}