package com.reservation.observability;

public class ExecutionContextHolder {

    private static final ThreadLocal<ExecutionContext> contextHolder = new ThreadLocal<>();

    public static void set(ExecutionContext context) {
        contextHolder.set(context);
    }

    public static ExecutionContext get() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }
}