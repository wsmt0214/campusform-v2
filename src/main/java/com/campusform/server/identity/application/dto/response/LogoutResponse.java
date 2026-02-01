package com.campusform.server.identity.application.dto.response;

import com.campusform.server.global.common.ResponseStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그아웃 응답 DTO (POST /api/auth/logout)
 */
@Schema(description = "로그아웃 응답")
public record LogoutResponse(
        @Schema(description = "응답 상태", example = "success")
        String status,
        @Schema(description = "응답 메시지", example = "로그아웃하였습니다.")
        String message
) {
    /**
     * 로그아웃 성공 응답 생성
     */
    public static LogoutResponse success() {
        return new LogoutResponse(ResponseStatus.SUCCESS.getValue(), "로그아웃하였습니다.");
    }
}
