package com.campusform.server.project.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Google OAuth2 authorization code 교환 요청 DTO
 * 
 * API 테스트용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeGoogleOAuthCodeRequest {

    // Google 로그인 후 발급되는 1회용 code 값
    private String code;

    // code 받을 때 사용한 redirect_uri와 동일
    private String redirectUri;
}
