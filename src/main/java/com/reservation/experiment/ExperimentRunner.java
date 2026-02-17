package com.reservation.experiment;

import com.reservation.service.strategy.ReservationStrategy;
import com.reservation.observability.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExperimentRunner implements CommandLineRunner {

    private final ReservationStrategy strategy;
    private final MetricsCollector metricsCollector;

    @Override
    public void run(String... args) throws Exception {

        System.out.println("=== 실험 시작 ===");

        metricsCollector.reset();

        Long concertId = 1L;

        for (int i = 0; i < 50; i++) {

            ExecutionContext context = new ExecutionContext();
            ExecutionContextHolder.set(context);

            try {
                strategy.reserve(concertId);
            } catch (Exception ignored) {
            } finally {
                metricsCollector.record(context);
                ExecutionContextHolder.clear();
            }
        }

        metricsCollector.printSummary();

        System.out.println("=== 실험 종료 ===");
    }
}
