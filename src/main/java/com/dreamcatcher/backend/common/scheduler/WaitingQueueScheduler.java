package com.dreamcatcher.backend.common.scheduler;

import com.dreamcatcher.backend.common.enums.SeatStatus;
import com.dreamcatcher.backend.seat.domain.Seat;
import com.dreamcatcher.backend.seat.domain.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingQueueScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;
    private final SeatRepository seatRepository;

    private static final String WAITING_QUEUE_KEY = "queue:waiting";
    private static final String ACTIVE_QUEUE_KEY = "queue:active";
    
    private static final String SCHEDULER_LOCK_PASS = "lock:scheduler:pass";
    private static final String SCHEDULER_LOCK_EVICT = "lock:scheduler:evict";

    private static final long PASS_COUNT = 100;
    private static final long ACTIVE_TTL_MILLIS = 10 * 60 * 1000;

    @Scheduled(cron = "0/10 * * * * *")
    public void passWaitingUsers() {
        RLock lock = redissonClient.getLock(SCHEDULER_LOCK_PASS);

        try {
            boolean isLocked = lock.tryLock(0, 5, TimeUnit.SECONDS);

            if (!isLocked) {
                return; 
            }

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

        } catch (InterruptedException e) {
            log.error("대기열 통과 스케줄러 락 획득 중 오류 발생", e);
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    @Scheduled(cron = "0/30 * * * * *")
    public void evictExpiredActiveUsers() {
        RLock lock = redissonClient.getLock(SCHEDULER_LOCK_EVICT);

        try {
            boolean isLocked = lock.tryLock(0, 10, TimeUnit.SECONDS);

            if (!isLocked) {
                return; 
            }

            long now = System.currentTimeMillis();
            
            // 1. 만료될 유저들의 ID를 먼저 조회 (삭제하기 전)
            Set<String> expiredUserIds = redisTemplate.opsForZSet().rangeByScore(ACTIVE_QUEUE_KEY, 0, now);
            
            if (expiredUserIds != null && !expiredUserIds.isEmpty()) {
                
                // 2. 이 유저들이 선점(RESERVED)해둔 좌석들을 찾아 AVAILABLE 로 롤백
                List<String> userIdList = expiredUserIds.stream().collect(Collectors.toList());
                List<Seat> reservedSeats = seatRepository.findByReservedByUserIdIn(userIdList);
                
                for (Seat seat : reservedSeats) {
                    if (seat.getSeatStatus() == SeatStatus.RESERVED) {
                        seat.setSeatStatus(SeatStatus.AVAILABLE);
                        seat.setReservedByUserId(null);
                        log.info("좌석 롤백 완료: Seat ID = {}, User ID = {}", seat.getSeatId(), expiredUserIds);
                    }
                }
                
                // 3. 롤백이 무사히 끝났으면 Redis 에서 만료된 유저 일괄 삭제
                Long removedCount = redisTemplate.opsForZSet().removeRangeByScore(ACTIVE_QUEUE_KEY, 0, now);
                log.info("========== 대기열 청소부 스케줄러 실행: 만료된 ACTIVE 유저 {}명 추방 및 미결제 좌석 {}개 롤백 완료 ==========", removedCount, reservedSeats.size());
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
