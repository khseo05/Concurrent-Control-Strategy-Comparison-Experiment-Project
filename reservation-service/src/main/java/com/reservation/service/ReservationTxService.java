package com.reservation.service;

import com.reservation.observability.*;
import com.reservation.service.strategy.*;
import com.reservation.domain.*;
import com.reservation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationTxService {

    private final ConcertRepository concertRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Reservation createPending(Long concertId) {

        long start = System.nanoTime();

        Concert concert = concertRepository.findById(concertId).orElseThrow();

        concert.decreaseSeat();

        Reservation reservation = new Reservation(concertId);

        Reservation saved = reservationRepository.save(reservation);

        long end = System.nanoTime();

        ExecutionContext context = ExecutionContextHolder.get();
        if (context != null) {
            context.addWriteTime(end - start);

        }

        return saved;
    }

    @Transactional
    public Reservation createPendingWithPessimisticLock(Long concertId) {
        long start = System.nanoTime();

        Concert concert = concertRepository.findByIdForUpdate(concertId).orElseThrow();

        concert.decreaseSeat();

        Reservation reservation = new Reservation(concertId);

        Reservation saved = reservationRepository.save(reservation);

        long end = System.nanoTime();

        ExecutionContext context = ExecutionContextHolder.get();
        if (context != null) {
            context.addWriteTime(end - start);
        }

        return saved;
    }

    @Transactional
    public void applyResult(Long reservationId, String result) {

        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();

        if (result.equals("SUCCESS")) {
            reservation.confirm();
            return;
        }

        if (result.equals("FAIL") || result.equals("TIMEOUT")) {
            if (reservation.cancel()) {
                Concert concert = concertRepository.findById(reservation.getConcertId()).orElseThrow();
                concert.increaseSeat();
            }
        }
    }
    @Transactional
    public void confirm(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        reservation.confirm();
    }

    @Transactional
    public void cancel(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();

        if (reservation.cancel()) {
            Concert concert = concertRepository.findById(reservation.getConcertId()).orElseThrow();
            
            concert.increaseSeat();
        }
    }

    @Transactional
    public void expire(Long reservationId) {

        Reservation reservation =
                reservationRepository.findById(reservationId).orElseThrow();

        if (reservation.expireIfNecessary()) {

            Concert concert =
                    concertRepository.findById(reservation.getConcertId())
                            .orElseThrow();

            concert.increaseSeat();
        }
    }  
}