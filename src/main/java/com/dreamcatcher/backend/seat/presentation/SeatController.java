package com.dreamcatcher.backend.seat.presentation;

import com.dreamcatcher.backend.seat.application.SeatFacade;
import com.dreamcatcher.backend.seat.application.SeatService;
import com.dreamcatcher.backend.seat.dto.SeatReservationResponse;
import com.dreamcatcher.backend.seat.dto.SeatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/seats")
public class SeatController {

    private final SeatFacade seatFacade;
    private final SeatService seatService;

    // 특정 콘서트 일정의 전체 좌석 조회 API
    @GetMapping("/concert-schedules/{concertScheduleId}")
    public ResponseEntity<List<SeatResponse>> getSeats(
            @PathVariable Long concertScheduleId,
            @RequestHeader("X-User-Id") String userId
    ) {
        List<SeatResponse> seats = seatService.getSeatsByConcertScheduleId(concertScheduleId, userId);
        return ResponseEntity.ok(seats);
    }

    // 좌석 임시 선점 (예매) API
    @PostMapping("/{seatId}/reserve")
    public ResponseEntity<SeatReservationResponse> reserveSeat(
            @PathVariable Long seatId, 
            @RequestHeader("X-User-Id") String userId
    ) {
        // Facade 에 있는 로직을 호출합니다.
        seatFacade.reserveSeat(seatId, userId);
        return ResponseEntity.ok(new SeatReservationResponse(seatId, userId, "좌석 선점이 완료되었습니다. 제한 시간 내에 결제를 진행해주세요."));
    }
}
