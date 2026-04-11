package com.reservation.gateway;

public interface GatewayClient {
    String process(String requestId, String resourceId, String resultType, int delayMs);
}
