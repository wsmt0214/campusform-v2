package com.campusform.server.identity.application.dto.response;

/**
 * 세션 유효 확인 응답 DTO (GET /api/auth/me)
 */
public record AuthMeResponse(
        boolean isAuthenticated,
        UserInfo user
) {
    /**
     * 사용자 정보
     */
    public record UserInfo(
            Long userId,
            String email,
            String nickname
    ) {}

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
