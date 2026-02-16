package com.reservation.reservation.payment;

public class TemporaryPaymentException extends PaymentException {
    public TemporaryPaymentException() {
        super("일시적 결제 장애");
    }
}