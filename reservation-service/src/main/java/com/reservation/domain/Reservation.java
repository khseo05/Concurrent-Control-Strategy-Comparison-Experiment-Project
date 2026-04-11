package com.reservation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long concertId;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private LocalDateTime createAt;
    private LocalDateTime expiresAt;

    public boolean expireIfNecessary() {
        if (this.status != ReservationStatus.PENDING) {
            return false;
        }

        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            this.status = ReservationStatus.EXPIRED;
            return true;
        }
        return false;
    }

    public void forceExpireNow() {
        this.expiresAt = LocalDateTime.now().minusMinutes(1);
    }


    public Reservation(Long concertId) {
        this.concertId = concertId;
        this.status = ReservationStatus.PENDING;
        this.createAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(5);
    }

    public boolean confirm() {
        if (this.status != ReservationStatus.PENDING) {
            return false;
        }
        this.status = ReservationStatus.CONFIRMED;
        return true;
    }

    public boolean cancel() {
        if (this.status != ReservationStatus.PENDING) {
            return false;
        }
        this.status = ReservationStatus.CANCELLED;
        return true;
    }
}