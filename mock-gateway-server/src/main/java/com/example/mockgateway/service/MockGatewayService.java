package com.example.mockgateway.service;

import com.example.mockgateway.enums.ResultType;

import org.springframework.stereotype.Service;

@Service
public class MockGatewayService {

    public String process(String resultType, int delayMs) {

        try {
            Thread.sleep(delayMs);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        ResultType type = ResultType.valueOf(resultType);

        if (type == ResultType.SUCCESS) {
            return "SUCCESS";
        }

        if (type == ResultType.FAIL) {
            return "FAIL";
        }

        return "TIMEOUT";
    }
}