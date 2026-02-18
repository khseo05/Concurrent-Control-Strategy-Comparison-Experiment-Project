package com.reservation.observability;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@Component
public class MetricsCollector {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong totalWriteTime = new AtomicLong();
    private final AtomicLong maxWriteTime = new AtomicLong();
    private final AtomicLong totalRetry = new AtomicLong();
    private final AtomicLong totalConflict = new AtomicLong();
    private final AtomicLong totalBlocked = new AtomicLong();
    private final List<Long> writeTimes = Collections.synchronizedList(new ArrayList<>());

    public void reset() {
        totalRequests.set(0);
        totalWriteTime.set(0);
        maxWriteTime.set(0);
        totalRetry.set(0);
        totalConflict.set(0);
        totalBlocked.set(0);
        writeTimes.clear();
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

    public void printSummary() {
        long requests = totalRequests.get();

        System.out.println("총 요청: " + requests);

        if (requests == 0) return;

        long p95 = calculatePercentile(0.95);
        long p99 = calculatePercentile(0.99);

        System.out.println("평균 write 시간(ns): " + totalWriteTime.get() / requests);
        System.out.println("최대 write 시간(ns): " + maxWriteTime.get());
        System.out.println("평균 retry: " + (double) totalRetry.get() / requests);
        System.out.println("충돌 횟수: " + totalConflict.get());
        System.out.println("차단 횟수: " + totalBlocked.get());
        System.out.println("P95 write 시간(ns): " + p95);
        System.out.println("P99 write 시간(ns): " + p99);
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
}