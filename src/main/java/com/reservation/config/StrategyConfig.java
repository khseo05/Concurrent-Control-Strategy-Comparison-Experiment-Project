package com.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.reservation.service.strategy.OptimisticReservationService;
import com.reservation.service.strategy.StateBasedReservationService;
import com.reservation.service.strategy.PessimisticReservationService;
import com.reservation.service.strategy.ReservationStrategy;


@Configuration
public class StrategyConfig {

    @Bean
    @Primary
    public ReservationStrategy reservationStrategy(
            StateBasedReservationService stateService
            //OptimisticReservationService optimisticService
            //PessimisticReservationService pessimisticService
    ) {
        //return optimisticService;
        //return pessimisticService;
        return stateService;
    }
}
