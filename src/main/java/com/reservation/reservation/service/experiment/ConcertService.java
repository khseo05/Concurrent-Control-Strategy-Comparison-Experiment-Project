package com.reservation.reservation.service.experiment;

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

    // 낙관적 락 + 재시도
    @Transactional
    public void reserveWithOptimisticLock(Long concertId) {

        int maxRetry = 10;

        for (int i = 0; i < maxRetry; i++) {
            try {
                Concert concert = concertRepository.findById(concertId)
                        .orElseThrow();
                concert.decreaseSeat();
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                System.out.println("충돌 발생 - 재시도");
            }
        }

        throw new IllegalStateException("충돌 과다 발생");
    }

    // 비관적 락
    @Transactional
    public void reserveWithPessimisticLock(Long concertId) {

        Concert concert = concertRepository
                .findByIdForUpdate(concertId)
                .orElseThrow();

        concert.decreaseSeat();
    }

    @Transactional
    public void reserve(Long concertId) {
        Concert concert = concertRepository.findById(concertId).orElseThrow();
        concert.decreaseSeat();
    }
}
