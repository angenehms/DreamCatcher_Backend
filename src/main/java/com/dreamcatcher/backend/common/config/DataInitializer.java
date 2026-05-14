package com.dreamcatcher.backend.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initData() {
        log.info("====== 데이터베이스 초기화 및 더미 데이터 세팅 시작 ======");

        // 1. 서버가 켜질 때마다 DB 초기화 (기존 데이터 삭제)
        // 외래키 제약조건을 잠시 해제하고 TRUNCATE 후 다시 설정합니다.
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;");
        jdbcTemplate.execute("TRUNCATE TABLE seats;");
        jdbcTemplate.execute("TRUNCATE TABLE concert_schedules;");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1;");

        // 2. 콘서트 일정 생성
        jdbcTemplate.update("INSERT INTO concert_schedules (concert_id, concert_date, concert_time, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                1L, "2026-12-25", "18:00", Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()));

        // 3. 무작위 예약 좌석 선정을 위한 리스트 셔플
        int totalSeats = 40;
        int reservedSeats = 12;
        
        List<Long> seatNumbers = new ArrayList<>();
        for (long i = 1; i <= totalSeats; i++) {
            seatNumbers.add(i);
        }
        // 1부터 40까지의 숫자를 무작위로 섞습니다.
        Collections.shuffle(seatNumbers);
        
        // 섞인 리스트에서 앞의 12개만 예약 좌석 번호로 사용합니다.
        List<Long> reservedSeatNumbers = seatNumbers.subList(0, reservedSeats);

        // 4. 좌석 40개 생성 (12개는 무작위로 예약됨, 나머지는 예매 가능)
        String sql = "INSERT INTO seats (concert_schedule_id, seat_number, seat_status, price, version, reserved_by_user_id, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batchArgs = new ArrayList<>();
        
        for (long i = 1; i <= totalSeats; i++) {
            String seatStatus;
            String reservedUserId;

            // 현재 생성하려는 좌석 번호(i)가 무작위로 뽑힌 예약 좌석 목록에 포함되어 있는지 확인
            if (reservedSeatNumbers.contains(i)) {
                seatStatus = "RESERVED";
                reservedUserId = "dummy_user_" + i; // 예약된 유저 ID 설정
            } else {
                seatStatus = "AVAILABLE";
                reservedUserId = null;
            }

            batchArgs.add(new Object[]{
                    1L,                                     // concert_schedule_id
                    i,                                      // seat_number
                    seatStatus,                             // seat_status
                    100000,                                 // price (10만원)
                    0,                                      // version (낙관적 락)
                    reservedUserId,                         // reserved_by_user_id
                    Timestamp.valueOf(LocalDateTime.now()), // created_at
                    Timestamp.valueOf(LocalDateTime.now())  // updated_at
            });
        }

        // JdbcTemplate을 이용한 Bulk Insert 실행!
        jdbcTemplate.batchUpdate(sql, batchArgs);

        log.info("====== JdbcTemplate Bulk Insert: 총 좌석 {}개 (무작위 예약됨 {}개, 예약가능 {}개) 세팅 완료! ======", totalSeats, reservedSeats, totalSeats - reservedSeats);
    }
}
