package com.reservation.observability;

public class ExecutionContext {

    private final long requestStartTime;

    private long totalWriteTime;
    private int retryCount;
    private int conflictCount;
    private int blockedCount;

    private long startTime;
    private long processingStartTime;
    private long endTime;

    private String status;
    private String message;
    private String strategyType;

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

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setProcessingStartTime(long processingStartTime) {
        this.processingStartTime = processingStartTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
    }
}