package com.reservation.reservation.repository;

import com.reservation.reservation.domain.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertRepository extends JpaRepository<Concert, Long> {
    
}