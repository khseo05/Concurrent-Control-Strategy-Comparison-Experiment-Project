package com.reservation.reservation.service;

import com.reservation.reservation.domain.Reservation;
import com.reservation.reservation.payment.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationTxService txService;
    private final PaymentService paymentService;

    public void reserve(Long concertId) {

        Reservation reservation = txService.createReservaton(concertId);

        int maxRetry = 3;
        int attempt = 0;
        
        while (attempt < maxRetry) {
            try {
                paymentService.callPayment();
                txService.confirm(reservation.getId());
                return;
            } catch (TemporaryPaymentException e) {
                attempt++;
                sleep(100);
            } catch (PermanentPaymentException e) {
                txService.cancel(reservation.getId());
                return;
            }
        }

        txService.cancel(reservation.getId());
    }

    private void sleep(long millis)  {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }
}