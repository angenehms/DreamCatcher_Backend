package com.dreamcatcher.backend.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingQueueScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    // 1. RedissonClient를 주입받아 분산 락을 사용할 준비를 합니다.
    private final RedissonClient redissonClient; 

    private static final String WAITING_QUEUE_KEY = "queue:waiting";
    private static final String ACTIVE_QUEUE_KEY = "queue:active";
    
    // 2. 스케줄러를 위한 락 이름을 정의합니다.
    private static final String SCHEDULER_LOCK_PASS = "lock:scheduler:pass";
    private static final String SCHEDULER_LOCK_EVICT = "lock:scheduler:evict";

    private static final long PASS_COUNT = 100;
    private static final long ACTIVE_TTL_MILLIS = 10 * 60 * 1000;

    @Scheduled(cron = "0/10 * * * * *")
    public void passWaitingUsers() {
        RLock lock = redissonClient.getLock(SCHEDULER_LOCK_PASS);

        try {
            // 3. [핵심] 대기 시간(waitTime)을 0초로 줍니다. 
            // "누군가 이미 락을 쥐고 있다면 1초도 기다리지 않고 즉시 포기하겠다"는 뜻입니다.
            // 임대 시간(leaseTime)은 5초로 설정하여 서버가 죽어도 5초 뒤엔 락이 풀리게 합니다.
            boolean isLocked = lock.tryLock(0, 5, TimeUnit.SECONDS);

            if (!isLocked) {
                // 락을 획득하지 못한 나머지 2대의 서버는 그냥 조용히 돌아갑니다.
                return; 
            }

            // --- 오직 락을 획득한 1대의 서버만 아래 로직을 실행합니다 ---
            Set<ZSetOperations.TypedTuple<String>> candidates = redisTemplate.opsForZSet()
                    .rangeWithScores(WAITING_QUEUE_KEY, 0, PASS_COUNT - 1);

            if (candidates == null || candidates.isEmpty()) {
                return;
            }

            log.info("========== 대기열 통과 스케줄러 실행 ({}명) ==========", candidates.size());

            long activeExpirationTime = System.currentTimeMillis() + ACTIVE_TTL_MILLIS;

            for (ZSetOperations.TypedTuple<String> tuple : candidates) {
                String userId = tuple.getValue();
                redisTemplate.opsForZSet().add(ACTIVE_QUEUE_KEY, userId, activeExpirationTime);
                redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY, userId);
            }
            // --- 안전 구역 끝 ---

        } catch (InterruptedException e) {
            log.error("대기열 통과 스케줄러 락 획득 중 오류 발생", e);
            Thread.currentThread().interrupt();
        } finally {
            // 4. 작업이 끝나면 락을 반납합니다.
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Scheduled(cron = "0/30 * * * * *")
    public void evictExpiredActiveUsers() {
        RLock lock = redissonClient.getLock(SCHEDULER_LOCK_EVICT);

        try {
            // 마찬가지로 0초 대기, 10초 유지로 락을 시도합니다.
            boolean isLocked = lock.tryLock(0, 10, TimeUnit.SECONDS);

            if (!isLocked) {
                return; 
            }

            long now = System.currentTimeMillis();
            Long removedCount = redisTemplate.opsForZSet().removeRangeByScore(ACTIVE_QUEUE_KEY, 0, now);

            if (removedCount != null && removedCount > 0) {
                log.info("========== 대기열 청소부 스케줄러 실행: 만료된 ACTIVE 유저 {}명 추방 완료 ==========", removedCount);
            }

        } catch (InterruptedException e) {
            log.error("대기열 청소 스케줄러 락 획득 중 오류 발생", e);
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
