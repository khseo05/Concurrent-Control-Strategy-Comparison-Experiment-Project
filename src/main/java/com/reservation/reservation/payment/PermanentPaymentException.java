package com.reservation.reservation.payment;

public class PermanentPaymentException extends PaymentException {
    public PermanentPaymentException() {
        super("결제 거절");
    }
}