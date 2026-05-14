package com.campusform.server.identity.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 세션 유효 확인 응답 DTO (GET /api/auth/me)
 */
@Schema(description = "현재 로그인된 사용자 정보 응답")
public record AuthMeResponse(
        @JsonProperty("isAuthenticated")
        @Schema(description = "인증 여부", example = "true") boolean isAuthenticated,
        @JsonProperty("isOnboarded")
        @Schema(description = "온보딩 완료 여부", example = "false") boolean isOnboarded,
        @Schema(description = "사용자 정보 (미인증 시 null)") UserInfo user) {

    @Schema(description = "로그인된 사용자 상세 정보")
    public record UserInfo(
            @Schema(description = "사용자 ID", example = "1") Long userId,
            @Schema(description = "이메일", example = "user@example.com") String email,
            @Schema(description = "닉네임", example = "캠퍼스폼") String nickname,
            @Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://example.com/profile.jpg") String profileImageUrl) {
    }

    public static AuthMeResponse authenticated(
            Long userId,
            String email,
            String nickname,
            String profileImageUrl,
            boolean isOnboarded) {
        return new AuthMeResponse(true, isOnboarded, new UserInfo(userId, email, nickname, profileImageUrl));
    }

    public static AuthMeResponse unauthenticated() {
        return new AuthMeResponse(false, false, null);
    }
}
