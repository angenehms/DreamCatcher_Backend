package com.dreamcatcher.backend.seat.presentation;

import com.dreamcatcher.backend.seat.application.SeatFacade;
import com.dreamcatcher.backend.seat.dto.SeatReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/seats")
public class SeatController {

    // Service 대신 Facade 를 주입받습니다.
    private final SeatFacade seatFacade;

    @PostMapping("/{seatId}/reserve")
    public ResponseEntity<SeatReservationResponse> reserveSeat(@PathVariable Long seatId, @RequestHeader("X-User-Id") String userId) {
        // Facade 에 있는 로직을 호출합니다.
        seatFacade.reserveSeat(seatId, userId);
        return ResponseEntity.ok(new SeatReservationResponse(seatId, userId, "좌석 선점이 완료되었습니다. 제한 시간 내에 결제를 진행해주세요."));
    }
}
