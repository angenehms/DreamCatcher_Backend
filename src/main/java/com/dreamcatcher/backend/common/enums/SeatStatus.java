package com.dreamcatcher.backend.common.enums;

public enum SeatStatus {
    AVAILABLE,
    HOLDING, // 예약을 시도 중인 HOLDING 상태를 Seat에도 추가하는 것이 논리적으로 매끄러움
    RESERVED
}
