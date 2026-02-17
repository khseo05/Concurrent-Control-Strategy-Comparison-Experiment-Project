package com.reservation.observability;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MetricsCollector {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong totalWriteTime = new AtomicLong();
    private final AtomicLong maxWriteTime = new AtomicLong();
    private final AtomicLong totalRetry = new AtomicLong();
    private final AtomicLong totalConflict = new AtomicLong();
    private final AtomicLong totalBlocked = new AtomicLong();

    public void reset() {
        totalRequests.set(0);
        totalWriteTime.set(0);
        maxWriteTime.set(0);
        totalRetry.set(0);
        totalConflict.set(0);
        totalBlocked.set(0);
    }

    public void record(ExecutionContext context) {
        totalRequests.incrementAndGet();

        long writeTime = context.getTotalWriteTime();
        totalWriteTime.addAndGet(writeTime);

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

        System.out.println("평균 write 시간(ns): " + totalWriteTime.get() / requests);
        System.out.println("최대 write 시간(ns): " + maxWriteTime.get());
        System.out.println("평균 retry: " + totalRetry.get() / requests);
        System.out.println("충돌 횟수: " + totalConflict.get());
        System.out.println("차단 횟수: " + totalBlocked.get());
    }
}