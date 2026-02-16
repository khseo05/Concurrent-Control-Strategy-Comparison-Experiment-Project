package com.reservation.reservation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Concert {

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;
    
    private int remainingSeats;

    public Concert(int remainingSeats) {
        this.remainingSeats = remainingSeats;
    }

    public void decreaseSeat() {
        if (remainingSeats <= 0) {
            throw new NoSeatException();
        }
        remainingSeats--;
    }

    public void increaseSeat() {
        remainingSeats++;
    }
}