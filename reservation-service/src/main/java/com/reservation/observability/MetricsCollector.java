package com.reservation.observability;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.FileWriter;
import java.io.IOException;

@Component
public class MetricsCollector {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong totalWriteTime = new AtomicLong();
    private final AtomicLong maxWriteTime = new AtomicLong();
    private final AtomicLong totalRetry = new AtomicLong();
    private final AtomicLong totalConflict = new AtomicLong();
    private final AtomicLong totalBlocked = new AtomicLong();
    private final AtomicLong totalIdempotencyHit = new AtomicLong();
    private final AtomicLong totalSuccess = new AtomicLong();
    private final AtomicLong totalFail = new AtomicLong();
    private final AtomicLong totalTimeout = new AtomicLong();
    private final List<Long> writeTimes = Collections.synchronizedList(new ArrayList<>());
    private volatile long experimentDurationMs = 1;

    public void reset() {
        totalRequests.set(0);
        totalWriteTime.set(0);
        maxWriteTime.set(0);
        totalRetry.set(0);
        totalConflict.set(0);
        totalBlocked.set(0);
        totalIdempotencyHit.set(0);
        totalSuccess.set(0);
        totalFail.set(0);
        totalTimeout.set(0);
        experimentDurationMs = 1;
        writeTimes.clear();
    }

    public void recordIdempotencyHit() {
        totalIdempotencyHit.incrementAndGet();
    }

    public void setExperimentDuration(long durationMs) {
        this.experimentDurationMs = durationMs;
    }

    public void record(ExecutionContext context) {
        totalRequests.incrementAndGet();

        long writeTime = context.getTotalWriteTime();
        totalWriteTime.addAndGet(writeTime);

        writeTimes.add(writeTime);
        updateMax(writeTime);

        totalRetry.addAndGet(context.getRetryCount());
        totalConflict.addAndGet(context.getConflictCount());
        totalBlocked.addAndGet(context.getBlockedCount());

        String status = context.getStatus();
        if ("SUCCESS".equals(status))     totalSuccess.incrementAndGet();
        else if ("FAIL".equals(status))   totalFail.incrementAndGet();
        else if ("TIMEOUT".equals(status)) totalTimeout.incrementAndGet();
    }

    private void updateMax(long writeTime) {
        long prev;

        do {
            prev = maxWriteTime.get();
            if (writeTime <= prev) {
                return;
            }
        } while (!maxWriteTime.compareAndSet(prev, writeTime));
    }

    public static class Summary {
        public long avg;
        public long max;
        public long p95;
        public long p99;
        public double avgRetry;
        public long totalConflict;
        public double tps;
        public double errorRate;
        public long successCount;
        public long failCount;
        public long timeoutCount;
    }

    public Summary getSummary() {
        long requests = totalRequests.get();

        Summary summary = new Summary();

        if (requests == 0) return summary;

        summary.avg = totalWriteTime.get() / requests;
        summary.max = maxWriteTime.get();
        summary.p95 = calculatePercentile(0.95);
        summary.p99 = calculatePercentile(0.99);
        summary.avgRetry = (double) totalRetry.get() / requests;
        summary.totalConflict = totalConflict.get();
        summary.tps = requests / (experimentDurationMs / 1000.0);
        summary.successCount = totalSuccess.get();
        summary.failCount = totalFail.get();
        summary.timeoutCount = totalTimeout.get();
        long errors = summary.failCount + summary.timeoutCount;
        summary.errorRate = (double) errors / requests * 100.0;

        return summary;
    }

    public void printSummary() {
        Summary s = getSummary();

        System.out.println("평균 write 시간(ns): " + s.avg);
        System.out.println("최대 write 시간(ns): " + s.max);
        System.out.println("평균 retry: " + s.avgRetry);
        System.out.println("충돌 횟수: " + s.totalConflict);
        System.out.println("P95 write 시간(ns): " + s.p95);
        System.out.println("P99 write 시간(ns): " + s.p99);
        System.out.printf("TPS: %.1f%n", s.tps);
        System.out.printf("에러율: %.1f%%%n", s.errorRate);
        System.out.printf("결과 분포: SUCCESS=%d, FAIL=%d, TIMEOUT=%d%n",
                s.successCount, s.failCount, s.timeoutCount);
        System.out.println("idempotency 차단: " + totalIdempotencyHit.get());
    }

    private long calculatePercentile(double percentile) {
        List<Long> copy;

        synchronized (writeTimes) {
            copy = new ArrayList<>(writeTimes);
        }

        if (copy.isEmpty()) return 0L;

        Collections.sort(copy);

        int index = (int) Math.ceil(percentile * copy.size()) -1;
        index = Math.max(0, Math.min(index, copy.size() -1));

        return copy.get(index);
    }

    public void exportCsv(String fileName, String strategy, String scenario, int threadCount) {

        Summary s = getSummary();

        try (FileWriter writer = new FileWriter(fileName, true)) {
            writer.write(String.format(
                "%s,%s,%d,%d,%d,%d,%d,%.2f,%d,%.1f,%.1f\n",
                strategy,
                scenario,
                threadCount,
                s.avg,
                s.p95,
                s.p99,
                s.max,
                s.avgRetry,
                s.totalConflict,
                s.tps,
                s.errorRate
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}