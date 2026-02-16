package com.reservation.reservation.domain;

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
        if (status == ReservationStatus.PENDING && expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            this.status = ReservationStatus.CANCELLED;
            return true;
        }
        return false;
    }

    public Reservation(Long concertId) {
        this.concertId = concertId;
        this.status = ReservationStatus.PENDING;
        this.createAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(5);
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }
}