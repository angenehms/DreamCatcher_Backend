package com.dreamcatcher.backend.payment.application;

import com.dreamcatcher.backend.common.enums.ReservationStatus;
import com.dreamcatcher.backend.common.enums.SeatStatus;
import com.dreamcatcher.backend.member.domain.Member;
import com.dreamcatcher.backend.member.domain.MemberRepository;
import com.dreamcatcher.backend.reservation.domain.Reservation;
import com.dreamcatcher.backend.reservation.domain.ReservationRepository;
import com.dreamcatcher.backend.seat.domain.Seat;
import com.dreamcatcher.backend.seat.domain.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String ACTIVE_QUEUE_KEY = "queue:active";

    @Transactional
    public void processPayment(Long seatId, Long userId) {

        // 1. [인가 로직] 대기열을 통과하여 예매/결제 권한이 있는 유저인지 확인 (TTL 만료 방어)
        checkActiveUser(String.valueOf(userId));

        // 2. 좌석 정보 확인 (이 좌석이 결제 가능한 RESERVED 상태인지 확인)
        // 실제로는 "이 좌석을 방금 예약(선점)한 사람이 나(userId)인지"를 추가로 검증해야 하지만, 
        // PoC로 만들고 있는 프로젝트라서 복잡도를 낮추기 위해 상태만 확인!
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        if (seat.getSeatStatus() != SeatStatus.RESERVED) {
            throw new IllegalStateException("아직 예약 선점되지 않은 좌석입니다. 선점(reserve)을 먼저 진행해주세요.");
        }

        // 3. 결제를 위한 유저 정보(잔액) 조회
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        log.info("[결제 시작] 유저: {}, 현재 잔액: {}, 결제할 금액: {}", userId, member.getPoint(), seat.getPrice());

        // 4. 잔액 확인 로직
        if (member.getPoint() < seat.getPrice()) {
            throw new IllegalArgumentException("포인트가 부족합니다. 결제를 진행할 수 없습니다.");
        }

        // 5. 포인트 차감 (이때 JPA가 @Version을 확인하여 낙관적 락을 작동시킴!)
        // Member 엔티티 안에 updatePoint 같은 비즈니스 메서드를 만드는 것이 객체지향적이지만, 임시로 setter를 사용합니다.
        member.setPoint(member.getPoint() - seat.getPrice());

        // 6. 예약 영수증(Reservation) 발행 및 저장
        Reservation reservation = new Reservation();
        reservation.setMemberId(userId);
        reservation.setSeatId(seatId);
        reservation.setReservationStatus(ReservationStatus.PAYMENT_COMPLETED);

        reservationRepository.save(reservation);

        log.info("[결제 완료] 유저 {}님의 결제가 완료되고 예약이 확정되었습니다. 남은 잔액: {}", userId, member.getPoint());
    }

    /**
     * (내부 메서드) 유저가 ACTIVE 상태인지 검증합니다.
     */
    private void checkActiveUser(String userId) {
        boolean isPassed = redisTemplate.opsForZSet().score(ACTIVE_QUEUE_KEY, userId) != null;
        if (!isPassed) {
            throw new IllegalStateException("대기열(결제 시간)이 만료되었거나 비정상적인 접근입니다.");
        }
    }
}
