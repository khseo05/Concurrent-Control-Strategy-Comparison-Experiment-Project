package com.reservation.reservation.controller;

import com.reservation.reservation.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/concerts")
public class ConcertController {

    private final ConcertService concertService;

    @PostMapping("/{id}/reserve")
    public String reserve(@PathVariable Long id) {
        concertService.reserve(id);
        return "예약 성공";
    }
}