package com.campusform.server.project.presentation;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.project.application.dto.request.ExchangeGoogleOAuthCodeRequest;
import com.campusform.server.project.application.dto.response.ExchangeGoogleOAuthCodeResponse;
import com.campusform.server.project.application.service.GoogleOAuthTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 구글 시트 접근 위해 필요한 권한을 획득하는 API
 * 1. 사용자가 구글 권한 요청을 보내면, 구글에서 인증 code를 발급
 * 2. 프론트엔드가 해당 code를 서버로 전달하면, 서버에서 access/refresh token으로 교환
 * 3. 얻은 토큰을 DB에 저장하고, 이후 구글 시트 API 호출 시 활용
 */
@RestController
@RequestMapping("/api/projects/google-oauth")
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthController {

    private final GoogleOAuthTokenService tokenService;
    private final AuthService authService;

    /**
     * Scope 권한 요청 -> 승인 후 code를 access_token과 refresh_token으로 교환
     * 
     * 권한 승인 후 Google이 redirectUri로 code를 전달했을 때, 프론트엔드가 이 엔드포인트로 전달
     * 
     * [에러 처리]
     * - 인증 실패: 401 Unauthorized
     * - Code 교환 실패: 500 Internal Server Error (Google API 오류)
     * - 잘못된 redirect_uri: Google에서 400 Bad Request 반환
     */
    @PostMapping("/exchange-code")
    public ResponseEntity<ExchangeGoogleOAuthCodeResponse> exchangeCode(
            @RequestBody ExchangeGoogleOAuthCodeRequest request,
            Authentication authentication) {

        log.info("Google OAuth2 code 교환 요청 - redirectUri: {}", request.getRedirectUri());

        // 인증된 사용자만 사용 가능
        Long userId = authService.extractUserId(authentication);

        // 구글 토큰 엔드포인트(https://oauth2.googleapis.com/token)에 Post 요청하여 Code를 Token으로 교환
        ExchangeGoogleOAuthCodeResponse response = tokenService.exchangeCode(request);

        // DB 토큰 테이블에 Upsert -> 이후 시트 API 호출 시 이 토큰 사용
        tokenService.saveToken(
                userId,
                response.getAccessToken(),
                response.getRefreshToken(),
                response.getExpiresIn());
        log.info("Google OAuth2 토큰 저장 완료 - userId: {}", userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Google Sheets 권한 요청 URL 생성
     * 
     * [호출 시점]
     * - 구글 시트 접근 시 권한이 필요한 경우 (예: 사용자가 구글 시트 연동 버튼 클릭 시)
     * - 사용자를 구글 권한 승인 페이지로 리다이렉트 하기 전 URL 받기 위해 사용
     */
    @GetMapping("/authorize-url")
    public ResponseEntity<Map<String, String>> getAuthorizeUrl() {
        // 구글 Scope(시트 권한) 요청 URL 생성
        String authorizeUrl = tokenService.buildAuthorizeUrl();

        /**
         * 이 링크를 토대로 프론트엔드가 리다이렉트
         * 
         * 왜 굳이 백엔드가 리다이렉트 안 시키고 프론트엔드가 리다이렉트 시키는가
         * 프론트가 직접 흐름을 제어해야 OAuth 완료 후 명확히 context를 관리할 수 있음
         */
        return ResponseEntity.ok(Map.of("authorizeUrl", authorizeUrl));
    }
}
