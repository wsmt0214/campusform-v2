package com.campusform.server.global.exception;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API 에러 응답 DTO
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {

    /** 에러 식별 코드 (예: UNAUTHORIZED, VALIDATION_ERROR) */
    private String code;

    /** 클라이언트에 노출할 에러 메시지 */
    private String message;

    /** 
     * 필드별 상세 검증 오류
     * @Valid 실패 시에만 사용, 그 외는 null 
     */
    private Map<String, String> details;
}
