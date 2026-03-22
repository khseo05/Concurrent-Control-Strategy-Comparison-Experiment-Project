package com.example.mockgateway.controller;

import com.example.mockgateway.dto.MockGatewayRequest;
import com.example.mockgateway.service.MockGatewayService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mock")
public class MockGatewayController {

    private final MockGatewayService mockGatewayService;

    public MockGatewayController(MockGatewayService mockGatewayService) {
        this.mockGatewayService = mockGatewayService;
    }
    
    @PostMapping("/process") 
    public String process(@RequestBody MockGatewayRequest request) {

        return mockGatewayService.process(
            request.getResultType(),
            request.getDelayMs()
        );
    }
}