package com.campusform.server.project.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Google OAuth2 authorization code 교환 응답 DTO
 */
@Schema(description = "Google OAuth 인증 코드 교환 응답")
@Getter
@RequiredArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExchangeGoogleOAuthCodeResponse {

    @Schema(description = "API 호출 시 사용할 Access Token")
    // API 호출용
    private final String accessToken;

    @Schema(description = "Access Token 갱신 시 사용할 Refresh Token")
    // AccessToken 갱신용
    private final String refreshToken;

    @Schema(description = "Access Token의 만료 시간(초 단위)", example = "3599")
    // AccessToken 만료 시간
    private final Integer expiresIn;

    @Schema(description = "토큰 타입", example = "Bearer")
    // 토큰 타입 (보통 Bearer)
    private final String tokenType;

    @Schema(description = "승인된 권한(scope) 목록", example = "https://www.googleapis.com/auth/spreadsheets")
    // 승인된 권한(scope) 목록
    private final String scope;
}
