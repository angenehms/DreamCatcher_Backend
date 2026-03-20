package com.dreamcatcher.backend.common.enums;

public enum ReservationStatus {
    PAYMENT_COMPLETED, // 결제완료
    HOLDING, // 임시홀딩 (예약이 진행되는 그 짧은 시간에 결제 대기 5분과 같은 기능을 넣고 싶다면) or 결제가 끝날 때까지 Lock 을 걸거면 없어도 됨
    CANCELLED // 취소됨
}
