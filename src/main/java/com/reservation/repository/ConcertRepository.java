package com.reservation.repository;

import com.reservation.domain.Concert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface ConcertRepository extends JpaRepository<Concert, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Concert c where c.id = id")
    Optional<Concert> findByIdForUpdate(@Param("id") Long id); 
}