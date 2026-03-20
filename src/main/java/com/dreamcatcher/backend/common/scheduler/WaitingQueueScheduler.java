package com.dreamcatcher.backend.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingQueueScheduler {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String WAITING_QUEUE_KEY = "queue:waiting";
    private static final String ACTIVE_QUEUE_KEY = "queue:active";

    // 한 번에 통과시킬 인원 수
    private static final long PASS_COUNT = 100;
    // Active 서랍에 머물 수 있는 제한 시간 (예: 10분 = 600,000 밀리초)
    private static final long ACTIVE_TTL_MILLIS = 10 * 60 * 1000;

    /**
     * 10초마다 실행되는 대기열 통과 스케줄러
     * cron = "0/10 * * * * *" : 매 10초마다 (0초, 10초, 20초...) 실행
     */
    @Scheduled(cron = "0/10 * * * * *")
    public void passWaitingUsers() {

        // 1. 대기열(Waiting)에서 가장 점수가 낮은(오래 기다린) 100명을 꺼냅니다. (0등부터 99등까지)
        Set<ZSetOperations.TypedTuple<String>> candidates = redisTemplate.opsForZSet()
                .rangeWithScores(WAITING_QUEUE_KEY, 0, PASS_COUNT - 1);

        // 뽑힌 사람이 없으면 (대기열이 비어있으면) 스케줄러 종료
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        log.info("========== 대기열 통과 스케줄러 실행 ({}명) ==========", candidates.size());

        // 2. 뽑힌 사람들을 차례대로 Active 서랍으로 옮기고, Waiting 서랍에서는 지웁니다.
        long activeExpirationTime = System.currentTimeMillis() + ACTIVE_TTL_MILLIS;

        for (ZSetOperations.TypedTuple<String> tuple : candidates) {
            String userId = tuple.getValue();

            // Active 서랍에 추가 (점수는 만료 시간으로 설정)
            redisTemplate.opsForZSet().add(ACTIVE_QUEUE_KEY, userId, activeExpirationTime);

            // Waiting 서랍에서 삭제
            redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY, userId);

            log.info("유저 {}님이 ACTIVE 상태로 전환되었습니다.", userId);
        }
    }

    /**
     * 30초마다 실행되는 만료자 청소부(TTL) 스케줄러 (Redis TTL 기능은 아니고 수동으로 기능만 흉내 구현)
     * cron = "0/30 * * * * *" : 매 30초마다 실행
     */
    @Scheduled(cron = "0/30 * * * * *")
    public void evictExpiredActiveUsers() {
        long now = System.currentTimeMillis();

        // Active 서랍(ZSET)에서 점수(Score)가 0부터 현재 시간(now)까지인 유저들을 모두 삭제
        // = "만료 시간이 이미 지나버린(과거가 된) 유저들을 삭제하라"
        Long removedCount = redisTemplate.opsForZSet().removeRangeByScore(ACTIVE_QUEUE_KEY, 0, now);

        if (removedCount != null && removedCount > 0) {
            log.info("========== 대기열 청소부 스케줄러 실행: 만료된 ACTIVE 유저 {}명 추방 완료 ==========", removedCount);
        }
    }

}
