package com.reservation.reservation.domain;

public class NoSeatException extends RuntimeException {
    public NoSeatException() {
        super("좌석 부족");
    }
}