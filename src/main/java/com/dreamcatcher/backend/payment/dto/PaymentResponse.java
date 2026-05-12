package com.dreamcatcher.backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentResponse {
    private final Long seatId;

    private final Long userId;

    private final String message;
}
