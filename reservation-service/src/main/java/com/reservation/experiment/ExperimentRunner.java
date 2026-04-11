package com.reservation.experiment;

import com.reservation.observability.MetricsCollector;
import com.reservation.domain.Concert;
import com.reservation.repository.ConcertRepository;
import com.reservation.service.ReservationService;
import com.reservation.service.strategy.OptimisticReservationService;
import com.reservation.service.strategy.PessimisticReservationService;
import com.reservation.service.strategy.ReservationStrategy;
import com.reservation.service.strategy.StateBasedReservationService;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class ExperimentRunner implements CommandLineRunner {

    private final ConcertRepository concertRepository;
    private final ReservationService reservationService;
    private final MetricsCollector metricsCollector;
    private final OptimisticReservationService optimisticService;
    private final PessimisticReservationService pessimisticService;
    private final StateBasedReservationService stateBasedService;

    record Scenario(String name, String resultType, int delayMs, boolean withDuplicates) {}

    private static final List<Scenario> SCENARIOS = List.of(
        new Scenario("success",              "SUCCESS", 100,  false),
        new Scenario("fail",                 "FAIL",    100,  false),
        new Scenario("timeout",              "TIMEOUT", 1500, false),
        new Scenario("success+idempotency",  "SUCCESS", 100,  true)
    );

    private static final int[] THREAD_COUNTS = {50, 100, 200};

    @Override
    public void run(String... args) throws Exception {

        Map<String, ReservationStrategy> strategies = new LinkedHashMap<>();
        strategies.put("optimistic",  optimisticService);
        strategies.put("pessimistic", pessimisticService);
        strategies.put("stateBased",  stateBasedService);

        for (Map.Entry<String, ReservationStrategy> entry : strategies.entrySet()) {
            for (Scenario scenario : SCENARIOS) {
                for (int threadCount : THREAD_COUNTS) {
                    runOne(entry.getKey(), entry.getValue(), scenario, threadCount);
                }
            }
        }
    }

    private void runOne(String strategyName, ReservationStrategy strategy,
                        Scenario scenario, int threadCount) throws Exception {

        System.out.printf("%n=== [%s] [%s] threads=%d ===%n",
                strategyName, scenario.name(), threadCount);

        metricsCollector.reset();
        concertRepository.deleteAll();
        Concert concert = concertRepository.save(new Concert(1000));
        Long concertId = concert.getId();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        int poolSize = scenario.withDuplicates() ? threadCount / 2 : threadCount;
        List<String> requestIds = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            requestIds.add(UUID.randomUUID().toString());
        }

        for (int i = 0; i < threadCount; i++) {
            String requestId = requestIds.get(i % poolSize);
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    reservationService.reserve(requestId, concertId, scenario.resultType(),
                            scenario.delayMs(), strategy);
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        doneLatch.await();
        metricsCollector.setExperimentDuration(System.currentTimeMillis() - startTime);
        executor.shutdown();

        metricsCollector.printSummary();
        metricsCollector.exportCsv("metrics.csv", strategyName, scenario.name(), threadCount);
    }
}
