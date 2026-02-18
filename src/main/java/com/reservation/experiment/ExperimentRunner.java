package com.reservation.experiment;

import com.reservation.observability.ExecutionContext;
import com.reservation.observability.ExecutionContextHolder;
import com.reservation.observability.MetricsCollector;
import com.reservation.service.strategy.ReservationStrategy;
import com.reservation.domain.Concert;
import com.reservation.repository.ConcertRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.io.FileWriter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ExperimentRunner implements CommandLineRunner {

    private final ConcertRepository concertRepository;
    private final ReservationStrategy strategy;
    private final MetricsCollector metricsCollector;

    @Override
    public void run(String... args) throws Exception {

        System.out.println("=== 실험 시작 ===");

        metricsCollector.reset();

        concertRepository.deleteAll();
        Concert concert = concertRepository.save(new Concert(1000));
        Long concertId = concert.getId();
        int threadCount = 200;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {

            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    ExecutionContext context = new ExecutionContext();
                    ExecutionContextHolder.set(context);

                    try {
                        strategy.reserve(concertId);
                    } catch (Exception ignored) {
                    } finally {
                        metricsCollector.record(context);
                        ExecutionContextHolder.clear();
                    }

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
