package com.reservation.observability;

public class ExecutionContext {
    
    private final long requestStartTime;

    private long totalWriteTime;
    private int retryCount;
    private int conflictCount;
    private int blockedCount;

    public ExecutionContext() {
        this.requestStartTime = System.nanoTime();
    }

    public void addWriteTime(long writeTime) {
        this.totalWriteTime += writeTime;
    }

    public void increaseRetry() {
        this.retryCount++;
    }

    public void increaseConflict() {
        this.conflictCount++;
    }

    public void increaseBlocked() {
        this.blockedCount++;
    }

    public long getTotalLatency() {
        return System.nanoTime() - requestStartTime;
    }

    public long getTotalWriteTime() {
        return totalWriteTime;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getConflictCount() {
        return conflictCount;
    }

    public int getBlockedCount() {
        return blockedCount;
    }
}