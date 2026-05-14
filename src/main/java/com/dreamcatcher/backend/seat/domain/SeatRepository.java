package com.dreamcatcher.backend.seat.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByReservedByUserIdIn(List<String> userIds);
    
    // 특정 콘서트 일정의 모든 좌석을 조회 (좌석 번호 오름차순)
    List<Seat> findByConcertScheduleIdOrderBySeatNumberAsc(Long concertScheduleId);
}
