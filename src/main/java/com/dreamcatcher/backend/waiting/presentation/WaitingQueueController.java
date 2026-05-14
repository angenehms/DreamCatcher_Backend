package com.dreamcatcher.backend.waiting.presentation;

import com.dreamcatcher.backend.waiting.application.WaitingQueueService;
import com.dreamcatcher.backend.waiting.dto.EnterQueueResponse;
import com.dreamcatcher.backend.waiting.dto.WaitingStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/waiting")
@RequiredArgsConstructor
public class WaitingQueueController {

    private final WaitingQueueService waitingQueueService;

    /**
     * 1. 대기열 진입 API (토큰 발급)
     * 클라이언트가 헤더에 X-User-Id를 담아 호출합니다.
     */
    @PostMapping
    public ResponseEntity<EnterQueueResponse> enterQueue(
            @RequestHeader("X-User-Id") String userId
    ) {
        waitingQueueService.enterQueue(userId);
        return ResponseEntity.ok(new EnterQueueResponse(userId, "대기열 진입 완료"));
    }

    /**
     * 2. 내 대기 상태 및 순번 확인 API (Polling)
     * 클라이언트가 3~5초마다 지속적으로 호출합니다.
     */
    @GetMapping("/status")
    public ResponseEntity<WaitingStatusResponse> getWaitingStatus(
            @RequestHeader("X-User-Id") String userId
    ) {
        WaitingStatusResponse status = waitingQueueService.getWaitingStatus(userId);
        return ResponseEntity.ok(status);
    }
}
