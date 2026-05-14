package com.dreamcatcher.backend.seat.application;

import com.dreamcatcher.backend.common.enums.SeatStatus;
import com.dreamcatcher.backend.seat.domain.Seat;
import com.dreamcatcher.backend.seat.domain.SeatRepository;
import com.dreamcatcher.backend.seat.dto.SeatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SeatRepository seatRepository;

    private static final String ACTIVE_QUEUE_KEY = "queue:active";

    // 특정 콘서트의 모든 좌석 정보를 조회하여 반환
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByConcertScheduleId(Long concertScheduleId, String userId) {
        
        // 대기열 통과 유저만 좌석 조회 가능 (선택 사항이지만 일관성을 위해 추가)
        checkActiveUser(userId);

        List<Seat> seats = seatRepository.findByConcertScheduleIdOrderBySeatNumberAsc(concertScheduleId);
        
        return seats.stream()
                .map(SeatResponse::from)
                .collect(Collectors.toList());
    }

    // 트랜잭션의 범위가 비즈니스 로직(DB 접근)으로만 한정됨
    @Transactional
    public void reserveSeatInTransaction(Long seatId, String userId) {
        
        // 1. 인가 로직 - 이 유저가 ACTIVE 서랍에 있는지 검사
        checkActiveUser(userId);

        log.info("[Seat: {}] 락 획득 성공! 예매 로직을 시작합니다. (User: {})", seatId, userId);

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        // 이중 검증
        if (seat.getSeatStatus() == SeatStatus.RESERVED) {
            log.warn("[Seat: {}] 이미 예매 완료된 좌석입니다. (User: {})", seatId, userId);
            throw new IllegalStateException("이미 다른 분이 예매를 완료한 좌석입니다.");
        }

        // 상태 변경 및 선점자 ID 기록
        seat.setSeatStatus(SeatStatus.RESERVED);
        seat.setReservedByUserId(userId);

        log.info("[Seat: {}] 예매 완료 (User: {})", seatId, userId);
    }

    private void checkActiveUser(String userId) {
        boolean isPassed = redisTemplate.opsForZSet().score(ACTIVE_QUEUE_KEY, userId) != null;
        if (!isPassed) {
            throw new IllegalStateException("대기열을 통과하지 않은 비정상적인 접근입니다.");
        }
    }
}
