package com.reservation.reservation.service;

import com.reservation.reservation.domain.Concert;
import com.reservation.reservation.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    public void reserve(Long concertId) {
        int maxRetry = 10;

        for (int i = 0; i < maxRetry; i++) {
            try {
                reserveOnce(concertId);
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                // 충돌 -> 재시도
            }
            throw new IllegalStateException("충돌 과다 발생");
        }
    }

    @Transactional
    public void reserveOnce(Long concertId) {
        Concert concert = concertRepository.findById(concertId).orElseThrow();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        concert.decreaseSeat();
    }
}