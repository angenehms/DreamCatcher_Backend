package com.dreamcatcher.backend.seat.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatFacade {

    private final RedissonClient redissonClient;
    private final SeatService seatService;

    private static final String LOCK_PREFIX = "lock:seat:";

    public void reserveSeat(Long seatId, String userId) {
        String lockName = LOCK_PREFIX + seatId;
        RLock lock = redissonClient.getLock(lockName);

        try {
            boolean isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("[Seat: {}] 이미 다른 사용자가 예매 중입니다. (User: {})", seatId, userId);
                throw new IllegalStateException("이미 다른 분이 예매 중인 좌석입니다.");
            }

            // 락 획득 성공 시, 트랜잭션을 가진 실제 비즈니스 로직 호출
            seatService.reserveSeatInTransaction(seatId, userId);

        } catch (InterruptedException e) {
            log.error("락 획득 대기 중 스레드에 문제가 발생했습니다.", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("예매 처리 중 서버 오류가 발생했습니다.");
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[Seat: {}] 락 반납 완료 (User: {})", seatId, userId);
            }
        }
    }
}
