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

    @Transactional
    public void reserve(Long concertId) {
        int maxRetry = 10;

        for (int i = 0; i < maxRetry; i++) {
            try {
                Concert concert = concertRepository.findById(concertId).orElseThrow();
                Thread.sleep(1000);
                concert.decreaseSeat();
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                System.out.println("충돌 발생 - 재시도");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("충돌 과다 발생");
    }
}