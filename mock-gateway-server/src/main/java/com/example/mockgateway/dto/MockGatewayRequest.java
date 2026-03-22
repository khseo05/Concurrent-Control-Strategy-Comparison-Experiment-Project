package com.example.mockgateway.dto;

public class MockGatewayRequest {

    private String requestId;
    private String resourceId;
    private String resultType;
    private int delayMs;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType; 
    }

    public int getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(int delayMs) {
        this.delayMs = delayMs;
    }
}