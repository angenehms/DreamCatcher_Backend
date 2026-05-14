package com.dreamcatcher.backend.seat.dto;

import com.dreamcatcher.backend.seat.domain.Seat;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeatResponse {

    private final Long seatId;
    private final Long concertScheduleId;
    private final Long seatNumber;
    private final String seatStatus;
    private final Integer price;
    private final String reservedByUserId;

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(
                seat.getSeatId(),
                seat.getConcertScheduleId(),
                seat.getSeatNumber(),
                seat.getSeatStatus().name(),
                seat.getPrice(),
                seat.getReservedByUserId()
        );
    }
}
