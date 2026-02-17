package com.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.reservation.service.strategy.OptimisticReservationService;
import com.reservation.service.strategy.ReservationStrategy;


@Configuration
public class StrategyConfig {

    @Bean
    @Primary
    public ReservationStrategy reservationStrategy(
            OptimisticReservationService optimisticService
    ) {
        return optimisticService;
    }
}
