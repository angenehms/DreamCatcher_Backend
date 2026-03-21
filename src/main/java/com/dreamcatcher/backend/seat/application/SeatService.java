package com.dreamcatcher.backend.seat.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {

    private final RedissonClient redissonClient;               // 분산 락을 걸기 위한 도구
    private final RedisTemplate<String, String> redisTemplate; // 대기열 검증을 위한 도구
    
    // (임시) 원래는 SeatRepository를 주입받아 DB 좌석 상태를 바꿔야 함
    // 하지만 지금은 락 획득 테스트에 집중하기 위해 Repository 코드는 생략
    // private final SeatRepository seatRepository;

    private static final String ACTIVE_QUEUE_KEY = "queue:active";
    private static final String LOCK_PREFIX = "lock:seat:"; // 락 이름 규칙

    @Transactional
    public void reserveSeat(Long seatId, String userId) {
        
        // 1. 인가 로직 - 이 유저가 ACTIVE 서랍에 있는지 (대기열을 통과했는지) 검사
        checkActiveUser(userId);

        // 2. Redisson을 이용해 해당 좌석에 대한 Lock 객체를 가져옴 ex) lock:seat:1
        String lockName = LOCK_PREFIX + seatId;
        RLock lock = redissonClient.getLock(lockName);

        try {
            // 3. Lock 채우기 (락 획득 시도)
            // 파라미터 -> 락을 기다릴 시간, 락을 유지할 시간, 시간 단위
            // 즉 최대 3초만 기다려보고, 자물쇠를 얻으면 5초 뒤에는 직접 안풀어도(네트워크 오류나도) 자동으로 풀기
            boolean isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);

            if (!isLocked) {
                // 3초를 기다렸는데도 자물쇠를 못 얻었다면? (이미 다른 사람이 예매 중)
                log.warn("[Seat: {}] 이미 다른 사용자가 예매 중입니다. (User: {})", seatId, userId);
                throw new IllegalStateException("이미 다른 분이 예매 중인 좌석입니다.");
            }

            // --- 여기부터는 '오직 1명'만 들어올 수 있는 완벽하게 안전한 구역 ---
            log.info("[Seat: {}] 락 획득 성공! 예매 로직을 시작합니다. (User: {})", seatId, userId);

            // 4. 진짜 비즈니스 로직 (DB 접근) 
            // - seatRepository.findById(seatId) 로 좌석을 찾고, 
            // - 상태가 AVAILABLE 인지 확인 후 RESERVED 로 바꿉니다.
            // 지금은 실제 DB 업데이트 대신 1초 대기하는 것으로 무거운 로직만 흉내내기
            Thread.sleep(1000); // 이게 실제 DB 접근 로직을 임시 대체한 것

            log.info("[Seat: {}] 예매 완료 (User: {})", seatId, userId);
            // --- 안전 구역 끝 ---

        } catch (InterruptedException e) {
            log.error("락 획득 대기 중 스레드에 문제가 발생했습니다.", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("예매 처리 중 서버 오류가 발생했습니다.");
        } finally {
            // 5. 작업이 끝나거나 예매가 터지거나 무조건! 자기가 건 자물쇠는 자기가 풀어야 함
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[Seat: {}] 락 반납 완료 (User: {})", seatId, userId);
            }
        }
    }

    /**
     * (내부 메서드) 유저가 ACTIVE 상태인지 검증합니다.
     */
    private void checkActiveUser(String userId) {
        boolean isPassed = redisTemplate.opsForZSet().score(ACTIVE_QUEUE_KEY, userId) != null;
        if (!isPassed) {
            throw new IllegalStateException("대기열을 통과하지 않은 비정상적인 접근입니다.");
        }
    }
}
