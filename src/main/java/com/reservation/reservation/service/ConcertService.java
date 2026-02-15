package com.reservation.reservation.service;

import com.reservation.reservation.domain.Concert;
import com.reservation.reservation.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    @Transactional
    public void reserve(Long concertId) {
        Concert concert = concertRepository.findByIdForUpdate(concertId).orElseThrow();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        concert.decreaseSeat();
    }
}