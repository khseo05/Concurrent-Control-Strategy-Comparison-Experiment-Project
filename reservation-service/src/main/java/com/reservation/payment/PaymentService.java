package com.reservation.payment;

import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public void callPayment() {
        double rand = Math.random();

        if (rand < 0.3) {
            throw new TemporaryPaymentException();
        } else if (rand < 0.6) {
            throw new PermanentPaymentException();
        }
    }
}
