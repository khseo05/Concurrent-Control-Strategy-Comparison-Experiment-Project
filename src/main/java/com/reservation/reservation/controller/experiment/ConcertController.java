package com.reservation.reservation.controller.experiment;

import com.reservation.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/concerts")
public class ConcertController {

    private final ReservationService reservationService;

    @PostMapping("/{id}/reserve")
    public String reserve(@PathVariable Long id) {
        reservationService.reserve(id);
        return "예약 성공";
    }
}