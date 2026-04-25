package com.dreamcatcher.backend.seat.application;

import com.dreamcatcher.backend.common.enums.SeatStatus;
import com.dreamcatcher.backend.seat.domain.Seat;
import com.dreamcatcher.backend.seat.domain.SeatRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SeatServiceConcurrentTest {

    @Autowired
    private SeatFacade seatFacade;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String ACTIVE_QUEUE_KEY = "queue:active";

    @AfterEach
    void tearDown() {
        seatRepository.deleteAll();
        redisTemplate.delete(ACTIVE_QUEUE_KEY);
    }

    @Test
    @DisplayName("100명의 유저가 동시에 1번 좌석을 예매하려고 하면, 1명만 성공하고 99명은 실패해야 한다.")
    void reserveSeat_concurrency_test() throws InterruptedException {
        // given: 1번 좌석을 미리 DB에 만들어 둡니다. (AVAILABLE 상태)
        Seat savedSeat = seatRepository.save(new Seat(null, 1L, 1L, SeatStatus.AVAILABLE, 100000, null, null));
        Long targetSeatId = savedSeat.getSeatId();

        // 100개의 스레드를 생성할 수 있는 스레드 풀 생성
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        
        // 모든 스레드가 작업을 마칠 때까지 기다리기 위한 자물쇠 (100개가 다 풀려야 끝남)
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 예매에 성공한 횟수와 실패한 횟수를 안전하게 셀 수 있는 변수
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when: 100명의 유저가 동시에 예매 버튼을 클릭한다고 가정 (스레드 100개 실행)
        for (int i = 0; i < threadCount; i++) {
            String userId = "user_" + i;
            
            // 테스트를 위해 모든 유저를 ACTIVE 상태(대기열 통과자)로 미리 만들어 둡니다.
            redisTemplate.opsForZSet().add(ACTIVE_QUEUE_KEY, userId, System.currentTimeMillis() + 60000);

            executorService.submit(() -> {
                try {
                    // 실제 핵심 로직! SeatFacade를 통해 락 획득 후 예매 시도
                    seatFacade.reserveSeat(targetSeatId, userId);
                    successCount.incrementAndGet(); // 에러 없이 끝났다면 성공 횟수 + 1
                } catch (Exception e) {
                    // 락을 못 얻었거나 이중 검증에서 튕겼다면 실패 횟수 + 1
                    failCount.incrementAndGet();
                    System.out.println("예약 실패: " + e.getMessage());
                } finally {
                    latch.countDown(); // 스레드 1개가 끝날 때마다 자물쇠를 하나씩 풂
                }
            });
        }

        // 모든 스레드가 끝날 때까지 (자물쇠 100개가 다 풀릴 때까지) 메인 스레드는 여기서 대기
        latch.await();

        // then: 테스트 결과 검증
        Seat finalSeat = seatRepository.findById(targetSeatId).orElseThrow();

        // 1. 단 1명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        // 2. 99명은 무조건 튕겨 나가야 함
        assertThat(failCount.get()).isEqualTo(99);
        // 3. 최종적으로 DB의 좌석 상태는 RESERVED 여야 함
        assertThat(finalSeat.getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
        // 4. 좌석을 선점한 유저 ID 필드(reservedByUserId)에 값이 채워져 있어야 함
        assertThat(finalSeat.getReservedByUserId()).isNotNull();
    }
}
