package com.campusform.server.project.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Google OAuth2 authorization code 교환 요청 DTO
 * 
 * API 테스트용
 */
@Schema(description = "Google OAuth 인증 코드 교환 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeGoogleOAuthCodeRequest {

    @Schema(description = "Google 로그인 후 리디렉션 URI로 받은 1회용 인증 코드", example = "4/0A...")
    // Google 로그인 후 발급되는 1회용 code 값
    private String code;

    @Schema(description = "인증 코드를 발급받을 때 사용한 리디렉션 URI (Google Cloud Console에 등록된 값과 일치해야 함)", example = "http://localhost:3000")
    // code 받을 때 사용한 redirect_uri와 동일
    private String redirectUri;
}
