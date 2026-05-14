package com.dreamcatcher.backend.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeatReservationResponse {
    private final Long seatId;

    private final String userId;

    private final String message;
}
