package com.dreamcatcher.backend.concert_schedule.domain;

import com.dreamcatcher.backend.common.auditing.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "concert_schedules")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ConcertSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="concert_schedule_id")
    private Long concertScheduleId;

    @Column(name="concert_id")
    private Long concertId;

    @Column(name="concert_date")
    private String concertDate;

    @Column(name="concert_time")
    private String concertTime;

} // end of ConcertSchedule