package com.campusform.server.project.presentation;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.campusform.server.global.security.CurrentUserId;
import com.campusform.server.project.application.dto.request.ExchangeGoogleOAuthCodeRequest;
import com.campusform.server.project.application.dto.response.ExchangeGoogleOAuthCodeResponse;
import com.campusform.server.project.application.service.GoogleOAuthTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 구글 시트 접근 위해 필요한 권한을 획득하는 API
 * 1. 사용자가 구글 권한 요청을 보내면, 구글에서 인증 code를 발급
 * 2. 프론트엔드가 해당 code를 서버로 전달하면, 서버에서 access/refresh token으로 교환
 * 3. 얻은 토큰을 DB에 저장하고, 이후 구글 시트 API 호출 시 활용
 */
@Tag(name = "Google 연동", description = "Google OAuth2 및 Sheets 연동 관련 API")
@RestController
@RequestMapping("/api/projects/google-oauth")
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthController {

    private final GoogleOAuthTokenService tokenService;

    /**
     * Scope 권한 요청 -> 승인 후 code를 access_token과 refresh_token으로 교환
     * 권한 승인 후 Google이 redirectUri로 code를 전달했을 때, 프론트엔드가 이 엔드포인트로 전달
     */
    @Operation(summary = "Google OAuth 인증 코드를 토큰으로 교환", description = "Google로부터 받은 인증 코드를 서버에 전달하여 Access Token 및 Refresh Token으로 교환하고 저장합니다.")
    @PostMapping("/exchange-code")
    public ResponseEntity<ExchangeGoogleOAuthCodeResponse> exchangeCode(
            @RequestBody ExchangeGoogleOAuthCodeRequest request,
            @CurrentUserId Long userId) {
        log.info("Google OAuth2 redirect URI debug - exchange-code requested. userId: {}, redirectUri: {}",
                userId, request.getRedirectUri());
        // 구글 토큰 엔드포인트(https://oauth2.googleapis.com/token)에 Post 요청하여 Code를 Token으로 교환
        ExchangeGoogleOAuthCodeResponse response = tokenService.exchangeCode(request);

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
     *
     * [useLocalhost] true면 콜백을 http://localhost:3000/oauth/google/callback 로 사용 (로컬
     * 테스트용).
     * false 또는 생략이면 app.oauth2.sheets-redirect-uri 사용.
     */
    @Operation(summary = "Google 권한 요청 URL 생성", description = "Google Sheets API 접근 권한을 얻기 위한 동의 화면 URL을 생성하여 반환합니다.", security = {})
    @GetMapping("/authorize-url")
    public ResponseEntity<Map<String, String>> getAuthorizeUrl(
            @RequestParam(required = false, defaultValue = "false") boolean useLocalhost) {
        log.info("Google OAuth2 redirect URI debug - authorize-url requested. useLocalhost: {}", useLocalhost);
        String authorizeUrl = tokenService.buildAuthorizeUrl(useLocalhost);
        return ResponseEntity.ok(Map.of("authorizeUrl", authorizeUrl));
    }
}
