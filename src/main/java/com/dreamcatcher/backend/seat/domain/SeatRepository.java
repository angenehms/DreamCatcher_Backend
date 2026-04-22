package com.dreamcatcher.backend.seat.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByReservedByUserIdIn(List<String> userIds);
}
