package com.reservation.reservation.controller;

import com.reservation.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public String reserve(@PathVariable Long id) {
        reservationService.reserve(id);
        return "예약 성공";
    }
}