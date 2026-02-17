package com.reservation.service.strategy;

import com.reservation.payment.TemporaryPaymentException;
import com.reservation.payment.PermanentPaymentException;
import com.reservation.domain.Reservation;
import com.reservation.payment.PaymentService;
import com.reservation.service.ReservationTxService;
import com.reservation.observability.ExecutionContext;
import com.reservation.observability.ExecutionContextHolder;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StateBasedReservationService implements ReservationStrategy {

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