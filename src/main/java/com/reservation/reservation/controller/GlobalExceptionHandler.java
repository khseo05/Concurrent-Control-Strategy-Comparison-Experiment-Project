package com.reservation.reservation;

import com.reservation.reservation.domain.NoSeatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class) 
    public ResponseEntity<String> handleNoseat(NoSeatException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("좌석 부족");
    }
}