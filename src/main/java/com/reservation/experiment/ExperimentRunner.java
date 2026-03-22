package com.reservation.experiment;

import com.reservation.observability.MetricsCollector;
import com.reservation.domain.Concert;
import com.reservation.repository.ConcertRepository;
import com.reservation.service.ReservationService;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class ExperimentRunner implements CommandLineRunner {

    private final ConcertRepository concertRepository;
    private final ReservationService reservationService;
    private final MetricsCollector metricsCollector;

    @Override
    public void run(String... args) throws Exception {

        System.out.println("=== 실험 시작 ===");

        metricsCollector.reset();

        concertRepository.deleteAll();
        Concert concert = concertRepository.save(new Concert(1000));
        Long concertId = concert.getId();
        int threadCount = 200;

        String resultType = "TIMEOUT";
        int delayMs = 100;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {

            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    reservationService.reserve(concertId, resultType, delayMs);

                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        executor.shutdown();

        metricsCollector.printSummary();
        metricsCollector.exportCsv("metrics.csv", "stateService", threadCount);

        System.out.println("=== 실험 종료 ===");
    }
}
