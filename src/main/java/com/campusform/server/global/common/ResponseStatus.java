package com.campusform.server.global.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 응답 상태 enum
 */
@Getter
@RequiredArgsConstructor
public enum ResponseStatus {
    SUCCESS("success"),
    FAILURE("failure");

    private final String value;
}
