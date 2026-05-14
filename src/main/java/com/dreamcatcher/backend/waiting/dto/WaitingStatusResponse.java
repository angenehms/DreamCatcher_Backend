package com.dreamcatcher.backend.waiting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WaitingStatusResponse {

    private final String status;

    private final Long aheadCount;

    private final String message;
}
