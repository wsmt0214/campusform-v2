package com.campusform.server.project.application.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Google OAuth2 authorization code 교환 응답 DTO
 */
@Getter
@RequiredArgsConstructor
public class ExchangeGoogleOAuthCodeResponse {

    // API 호출용
    private final String accessToken;

    // AccessToken 갱신용
    private final String refreshToken;

    // AccessToken 만료 시간
    private final Integer expiresIn;

    // 토큰 타입 (보통 Bearer)
    private final String tokenType;

    // 승인된 권한(scope) 목록
    private final String scope;
}
