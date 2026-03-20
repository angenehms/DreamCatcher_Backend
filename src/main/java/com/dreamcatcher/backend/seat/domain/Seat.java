package com.dreamcatcher.backend.seat.domain;

import com.dreamcatcher.backend.common.auditing.BaseEntity;
import com.dreamcatcher.backend.common.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seats")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Seat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="seat_id")
    private Long seatId;

    @Column(name="concert_schedule_id", nullable = false)
    private Long concertScheduleId;

    @Column(name="seat_number", nullable = false)
    private Long seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name="seat_status", nullable = false)
    private SeatStatus seatStatus;

    @Column(name="price", nullable = false)
    private Integer price;

    @Version
    @Column(name="version")
    private Integer version; // 분산락의 예외처리 누락으로 발생되는 엣지한 경우 방지하기 위해 낙관적 락도 함께 구현하기 위함

} // end of Seat

