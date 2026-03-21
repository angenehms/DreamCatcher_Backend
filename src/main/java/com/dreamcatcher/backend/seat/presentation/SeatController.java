package com.dreamcatcher.backend.seat.presentation;

import com.dreamcatcher.backend.seat.application.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    /**
     * 좌석 예매 (선점) API
     * 클라이언트가 좌석 번호(seatId)를 클릭했을 때 호출됩니다.
     */
    @PostMapping("/{seatId}/reserve")
    public ResponseEntity<String> reserveSeat(
            @PathVariable Long seatId,
            @RequestHeader("X-User-Id") String userId
    ) {
        // 서비스로 로직을 넘깁니다. (성공하면 예약 완료 메시지를, 실패하면 예외)
        seatService.reserveSeat(seatId, userId);
        return ResponseEntity.ok("좌석(" + seatId + ") 예약(선점)에 성공했습니다! 결제를 진행해 주세요.");
    }
}
