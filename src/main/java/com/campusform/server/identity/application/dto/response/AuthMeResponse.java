package com.campusform.server.identity.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 세션 유효 확인 응답 DTO (GET /api/auth/me)
 */
@Schema(description = "현재 로그인된 사용자 정보 응답")
public record AuthMeResponse(
        @Schema(description = "인증 여부", example = "true") boolean isAuthenticated,
        @Schema(description = "사용자 정보 (미인증 시 null)") UserInfo user) {
    /**
     * 사용자 정보
     */
    @Schema(description = "로그인된 사용자 상세 정보")
    public record UserInfo(
            @Schema(description = "사용자 ID", example = "1") Long userId,
            @Schema(description = "이메일", example = "user@example.com") String email,
            @Schema(description = "닉네임", example = "캠퍼스폼") String nickname) {
    }

    /**
     * 인증된 사용자 응답 생성
     */
    public static AuthMeResponse authenticated(Long userId, String email, String nickname) {
        return new AuthMeResponse(true, new UserInfo(userId, email, nickname));
    }

    /**
     * 미인증 사용자 응답 생성
     */
    public static AuthMeResponse unauthenticated() {
        return new AuthMeResponse(false, null);
    }
}
