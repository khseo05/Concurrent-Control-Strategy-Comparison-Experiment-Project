package com.reservation.reservation;

import com.reservation.reservation.domain.Concert;
import com.reservation.reservation.repository.ConcertRepository;
import com.reservation.reservation.service.ConcertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class ConcertServiceConcurrencyTest {

    @Autowired
    private ConcertService concertService;

    @Autowired
    private ConcertRepository concertRepository;

    private Long concertId;

    @BeforeEach
    void setUp() {
        Concert concert = new Concert(1); // 좌석 1개
        concertRepository.save(concert);
        concertId = concert.getId();
    }

    @Test
    void 동시에_100명_예약시_음수가_되는지_확인() throws InterruptedException {

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() ->  {
                try {
                    concertService.reserve(concertId);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            }); 
        }

        latch.await();

        Concert concert = concertRepository.findById(concertId).orElseThrow();
        System.out.println("남은 좌석: " + concert.getRemainingSeats());
        assertTrue(concert.getRemainingSeats() < 0);
    }
}