package com.dreamcatcher.backend.reservation.domain;

import com.dreamcatcher.backend.common.auditing.BaseEntity;
import com.dreamcatcher.backend.common.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reservations")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="reservation_id")
    private Long reservationId;

    @Column(name="member_id", nullable = false)
    private Long memberId;

    @Column(name="seat_id", nullable = false)
    private Long seatId;

    @Column(name="reservation_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus reservationStatus;


} // end of Reservation
