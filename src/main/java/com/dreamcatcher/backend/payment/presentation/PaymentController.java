package com.dreamcatcher.backend.payment.presentation;

import com.dreamcatcher.backend.payment.application.PaymentService;
import com.dreamcatcher.backend.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 및 예약 확정 API
     * 유저가 선점한 좌석에 대해 포인트를 차감하고 예약을 최종 확정합니다.
     */
    @PostMapping("/seats/{seatId}")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable Long seatId,
            @RequestHeader("X-User-Id") Long userId // Member 엔티티 조회를 위해 Long 타입으로 변경 (설계에 맞춤)
    ) {
        paymentService.processPayment(seatId, userId);
        return ResponseEntity.ok(new PaymentResponse(seatId, userId, "결제가 완료되고 예약이 최종 확정되었습니다!"));
    }
}
