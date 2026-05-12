package com.dreamcatcher.backend.waiting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EnterQueueResponse {

    private final String userId;

    private final String message;
}
