package com.dreamcatcher.backend.waiting.application;

import com.dreamcatcher.backend.waiting.dto.WaitingStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WaitingQueueService {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis Key 이름 (오타 방지 위해 상수로 관리)
    private static final String WAITING_QUEUE_KEY = "queue:waiting"; // 대기 중인 줄
    private static final String ACTIVE_QUEUE_KEY = "queue:active";   // 통과(Active)한 줄 (예매 가능)

    /**
     * 유저를 대기열(ZSET) 맨 뒤에 줄 세웁니다.
     */
    public void enterQueue(String userId) {

        // 1. 이미 통과한 유저인지 확인 (active 서랍에 있는지)
        boolean isPassed = redisTemplate.opsForZSet().score(ACTIVE_QUEUE_KEY, userId) != null;
        if (isPassed) return; // 이미 통과했다면(active 서랍에 있다면) 더 이상 대기열에 넣지 않음

        // 2. 대기열(waiting) 서랍에 삽입
        // 현재 타임스탬프를 점수(Score)로 줘서 삽입 (점수가 낮을수록 앞으로 옴)
        long currentTime = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(WAITING_QUEUE_KEY, userId, currentTime);
    }

    /**
     * 유저의 현재 상태(WAITING or ACTIVE)와 순번을 알려줍니다.
     * 클라이언트가 Polling 하기 위한 메서드
     */
    public WaitingStatusResponse getWaitingStatus(String userId) {

        // 1. 이미 통과한 유저인지 확인 (active 서랍에 있는지)
        boolean isPassed = redisTemplate.opsForZSet().score(ACTIVE_QUEUE_KEY, userId) != null;
        if (isPassed) {
            return new WaitingStatusResponse("ACTIVE", 0L, "접속 가능 - 예매를 진행해 주세요!");
        }

        // 2. 대기자(Waiting) 서랍에서 내 순위를 조회 (0번부터 시작)
        Long rank = redisTemplate.opsForZSet().rank(WAITING_QUEUE_KEY, userId);

        if (rank != null) {
            // rank가 0이면 내 앞에 0명(즉, 내가 1빠)이라는 뜻입니다.
            return new WaitingStatusResponse("WAITING", rank, "현재 대기 중입니다.");
        }

        // 3. 두 서랍 모두에 없으면 대기열 진입조차 안 한 상태입니다.
        return new WaitingStatusResponse("NOT_FOUND", -1L, "대기열 진입(POST)을 먼저 호출해 주세요.");
    }
}
