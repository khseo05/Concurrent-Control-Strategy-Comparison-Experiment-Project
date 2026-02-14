package com.reservation.reservation;

import com.reservation.reservation.domain.Concert;
import com.reservation.reservation.repository.ConcertRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final ConcertRepository concertRepository;

    @PostConstruct
    public void init() {
        concertRepository.save(new Concert(1));
    }
}