package com.reservation.service;

import com.reservation.observability.MeasureTx;
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
    @MeasureTx
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