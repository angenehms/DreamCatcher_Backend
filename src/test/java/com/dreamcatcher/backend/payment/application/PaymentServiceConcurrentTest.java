package com.dreamcatcher.backend.payment.application;

import com.dreamcatcher.backend.common.enums.SeatStatus;
import com.dreamcatcher.backend.member.domain.Member;
import com.dreamcatcher.backend.member.domain.MemberRepository;
import com.dreamcatcher.backend.seat.domain.Seat;
import com.dreamcatcher.backend.seat.domain.SeatRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PaymentServiceConcurrentTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String ACTIVE_QUEUE_KEY = "queue:active";

    @AfterEach
    void tearDown() {
        seatRepository.deleteAll();
        memberRepository.deleteAll();
        redisTemplate.delete(ACTIVE_QUEUE_KEY);
    }

    @Test
    @DisplayName("한 명의 유저가 동시에 두 번 결제 요청을 보내면(따닥), 낙관적 락에 의해 1번만 성공하고 1번은 튕겨야 한다.")
    void processPayment_concurrency_test() throws InterruptedException {
        // given: 돈이 10만 원 있는 유저 1번을 생성합니다.
        Member member = new Member();
        member.setNickname("Tester");
        member.setPoint(100000L);
        member.setVersion(0);
        Member savedMember = memberRepository.save(member);
        Long userId = savedMember.getMemberId();

        // 10만 원짜리 좌석을 만들어두고, 상태는 RESERVED로 미리 세팅해둡니다. (선점 완료 상태)
        Seat seat = new Seat();
        seat.setConcertScheduleId(1L);
        seat.setSeatNumber(1L);
        seat.setSeatStatus(SeatStatus.RESERVED);
        seat.setPrice(100000);
        seat.setVersion(0);
        Seat savedSeat = seatRepository.save(seat);
        Long targetSeatId = savedSeat.getSeatId();

        // 결제를 진행하려면 유저가 ACTIVE 상태여야 하므로 Redis에 미리 등록해 둡니다.
        redisTemplate.opsForZSet().add(ACTIVE_QUEUE_KEY, String.valueOf(userId), System.currentTimeMillis() + 60000);

        // when: 2개의 스레드(스마트폰, PC 동시 클릭 상황)로 동시에 결제 요청을 날립니다.
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 실제 결제 로직 호출 (JPA @Version 이 감시 중!)
                    paymentService.processPayment(targetSeatId, userId);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    // 낙관적 락에 의해 충돌이 감지되면 이 예외가 터져야 정상!
                    failCount.incrementAndGet();
                    System.out.println("낙관적 락 발동! 중복 결제 차단됨.");
                } catch (Exception e) {
                    System.out.println("기타 에러: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then: 테스트 결과 검증
        Member finalMember = memberRepository.findById(userId).orElseThrow();

        // 1. 단 1건의 결제만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        // 2. 다른 1건은 낙관적 락에 걸려 튕겨 나가야 함
        assertThat(failCount.get()).isEqualTo(1);
        // 3. 유저의 잔액은 10만 원이 두 번 깎이는(마이너스 통장) 대참사 없이, 딱 1번만 깎여서 0원이어야 함
        assertThat(finalMember.getPoint()).isEqualTo(0L);
        // 4. 낙관적 락이 작동했으므로 엔티티의 버전(version)은 1로 올라가 있어야 함
        assertThat(finalMember.getVersion()).isEqualTo(1);
    }
}
