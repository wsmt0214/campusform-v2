package com.campusform.server.project.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.campusform.server.project.application.dto.request.ExchangeGoogleOAuthCodeRequest;
import com.campusform.server.project.application.dto.response.ExchangeGoogleOAuthCodeResponse;
import com.campusform.server.project.domain.exception.TokenExpiredException;
import com.campusform.server.project.domain.exception.TokenNotFoundException;
import com.campusform.server.project.domain.model.sheet.GoogleOAuthToken;
import com.campusform.server.project.domain.repository.GoogleOAuthTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google OAuth2 토큰 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthTokenService {

    private final GoogleOAuthTokenRepository tokenRepository;
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${app.oauth2.sheets-redirect-uri:http://localhost:3000/oauth/google/callback}")
    private String defaultRedirectUri;

    /**
     * scope 권한을 승인받은 후 code를 access_token와 refresh_token으로 교환
     */
    @Transactional
    public ExchangeGoogleOAuthCodeResponse exchangeCode(ExchangeGoogleOAuthCodeRequest request) {
        log.info("Google OAuth2 code 교환 시작 - code: {}, redirectUri: {}",
                request.getCode().substring(0, Math.min(20, request.getCode().length())) + "...",
                request.getRedirectUri());

        // 구글 토큰 엔드포인트 URL
        String tokenUrl = "https://oauth2.googleapis.com/token";

        // 요청 헤더 설정 (참고: form-urlencoded 형식만 지원)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", request.getCode()); // 승인 code
        body.add("client_id", clientId); // Google OAuth2 클라이언트 ID
        body.add("client_secret", clientSecret); // Google OAuth2 클라이언트 시크릿 (보안상 중요)
        body.add("redirect_uri", request.getRedirectUri()); // 권한 요청 시 사용한 redirect_uri와 정확히 일치해야 함
        body.add("grant_type", "authorization_code"); // 고정값: authorization_code 방식
        /**
         * [redirect_uri 주의사항]
         * - code -> 토큰, 교환 시 redirect_uri는 최초 권한 요청 시 사용한 uri와 동일해야 함
         * - Google Cloud Console의 승인된 redirect_uri에도 동일 값이 등록되어야 함
         */

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            // POST 요청. RestTemplate를 사용해 동기 요청
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            /**
             * 응답 예시
             * {
             * "access_token": "ya29.a0AfH6SMBx...",
             * "refresh_token": "1//0gX7Yq...",
             * "expires_in": 3600,
             * "token_type": "Bearer",
             * "scope": "https://www.googleapis.com/auth/spreadsheets"
             * }
             */
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new IllegalStateException("Google OAuth2 토큰 교환 응답이 비어있습니다.");
            }

            /**
             * 응답 파싱
             */
            String accessToken = (String) responseBody.get("access_token");
            String refreshToken = (String) responseBody.get("refresh_token");
            Integer expiresIn = (Integer) responseBody.get("expires_in");
            String tokenType = (String) responseBody.get("token_type");
            String scope = (String) responseBody.get("scope");

            if (accessToken == null) {
                throw new IllegalStateException("Google OAuth2 토큰 교환 실패: " + responseBody);
            }

            log.info("Google OAuth2 code 교환 성공 - expiresIn: {}초, scope: {}", expiresIn, scope);

            return new ExchangeGoogleOAuthCodeResponse(
                    accessToken,
                    refreshToken,
                    expiresIn,
                    tokenType != null ? tokenType : "Bearer",
                    scope);

        } catch (HttpClientErrorException e) {
            // 401/400 등 Google이 반환한 상태코드와 응답 본문(error, error_description)을 로그·메시지에 포함
            String responseBody = e.getResponseBodyAsString();
            log.error("Google OAuth2 code 교환 실패 status={} body={}", e.getStatusCode(), responseBody, e);
            String detail = responseBody != null && !responseBody.isEmpty()
                    ? responseBody
                    : e.getMessage();
            throw new IllegalStateException("Google OAuth2 토큰 교환 중 오류가 발생했습니다: " + detail, e);
        } catch (Exception e) {
            log.error("Google OAuth2 code 교환 실패", e);
            throw new IllegalStateException("Google OAuth2 토큰 교환 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * access_token을 DB에 Upsert -> 이후 시트 API 호출 시 이 토큰 사용
     */
    @Transactional
    public void saveToken(Long ownerId, String accessToken, String refreshToken, Integer expiresIn) {
        /**
         * 만료 시간 계산
         * expiresIn (초)을 LocalDateTime으로 변환
         * 현재 시간 + expiresIn = expiryAt
         */
        LocalDateTime expiryAt = expiresIn != null
                ? LocalDateTime.now().plusSeconds(expiresIn)
                : null;

        /**
         * 기존 토큰 확인
         * 
         * 기존 토큰 존재 + 만료 또는 갱신됨 -> updateToken()
         * 토큰 없음 -> GoogleOAuthToken.create()로 새로 생성
         * 
         * 보통 access_token은 1시간 후 만료, refresh_token은 1달 후 만료(재로그인 필요)
         * refresh_token 만료 전 새 access_token 발급 가능
         */
        tokenRepository.findByOwnerId(ownerId)
                .ifPresentOrElse(
                        existing -> {
                            existing.updateToken(accessToken, refreshToken, expiryAt);
                            tokenRepository.save(existing);
                        },
                        () -> {
                            GoogleOAuthToken newToken = GoogleOAuthToken.create(
                                    ownerId, accessToken, refreshToken, expiryAt);
                            tokenRepository.save(newToken);
                        });

        log.info("Google OAuth2 토큰 저장 완료 - ownerId: {}", ownerId);
    }

    /**
     * Google Sheets 권한 요청 URL 생성
     *
     * @param useLocalhost true면 http://localhost:3000/oauth/google/callback 로 콜백 분기
     *                     false면 기본값(sheets-redirect-uri) 사용
     * @return 구글 동의 화면 URL
     */
    public String buildAuthorizeUrl(boolean useLocalhost) {
        String redirectUri = useLocalhost ? "http://localhost:3000/oauth/google/callback" : defaultRedirectUri;
        return buildAuthorizeUrlWithRedirectUri(redirectUri);
    }

    private String buildAuthorizeUrlWithRedirectUri(String redirectUri) {
        String scope = "https://www.googleapis.com/auth/spreadsheets";
        // URL 인코딩 -> 한글, 특수문자 등 올바르게 처리 보장
        String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8);
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        // 최종 구글 권한 요청 URL 생성
        return String.format(
                "https://accounts.google.com/o/oauth2/v2/auth?" +
                        "client_id=%s&" + // Google Cloud Console로부터 발급받은 클라이언트 ID
                        "redirect_uri=%s&" + // Cloud Console에 등록된 URI와 정확히 일치 필수. code 교환 시에도 동일하게 사용해야 함
                        "response_type=code&" + // authorization code 방식
                        "scope=%s&" + // 요청 권한
                        "access_type=offline&" + // 더불어 refresh_token 받기 위해 필수.
                        "prompt=consent", // 항상 권한 승인 화면 표시, 없으면 이미 승인한 경우 refresh_token을 받지 못할 수 있음
                clientId, encodedRedirectUri, encodedScope);
    }

    /**
     * 토큰 존재 여부 및 만료 시간 검증
     */
    public boolean hasValidToken(Long ownerId) {
        Optional<GoogleOAuthToken> tokenOpt = tokenRepository.findByOwnerId(ownerId);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        GoogleOAuthToken token = tokenOpt.get();

        // expiryAt이 null이면 만료 시간 정보가 없으므로 유효하지 않음
        if (token.getExpiryAt() == null) {
            return false;
        }

        // 현재 시간이 만료 시간보다 이전이면 유효
        return token.getExpiryAt().isAfter(LocalDateTime.now());
    }

    /**
     * 유효한 토큰 조회
     * 토큰이 만료되었지만 Refresh Token이 있으면 자동으로 갱신
     * 
     * @throws TokenNotFoundException 토큰이 없을 때
     * @throws TokenExpiredException  Refresh Token도 만료되었거나 없을 때
     */
    @Transactional
    public Optional<GoogleOAuthToken> getValidToken(Long ownerId) {
        Optional<GoogleOAuthToken> tokenOpt = tokenRepository.findByOwnerId(ownerId);

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        GoogleOAuthToken token = tokenOpt.get();

        // 토큰이 유효하면 그대로 반환
        if (token.getExpiryAt() != null && token.getExpiryAt().isAfter(LocalDateTime.now())) {
            return Optional.of(token);
        }

        // 토큰이 만료되었지만 Refresh Token이 있으면 갱신 시도
        if (token.getRefreshToken() != null && !token.getRefreshToken().isEmpty()) {
            try {
                refreshAccessToken(ownerId);
                // 갱신 후 다시 조회
                return tokenRepository.findByOwnerId(ownerId);
            } catch (Exception e) {
                log.error("토큰 갱신 실패 - ownerId: {}", ownerId, e);
                throw new TokenExpiredException("토큰이 만료되었고 갱신할 수 없습니다. 다시 인증이 필요합니다.");
            }
        }

        // Refresh Token이 없으면 만료된 것으로 간주
        throw new TokenExpiredException("토큰이 만료되었고 Refresh Token이 없습니다. 다시 인증이 필요합니다.");
    }

    /**
     * Refresh Token을 사용하여 Access Token 갱신
     * Google Token Endpoint에 POST 요청하여 새로운 Access Token을 발급
     * 
     * @throws TokenNotFoundException 토큰이 없을 때
     * @throws TokenExpiredException  Refresh Token이 없거나 갱신 실패 시
     */
    @Transactional
    public String refreshAccessToken(Long ownerId) {
        // 기존 토큰 조회
        GoogleOAuthToken token = tokenRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new TokenNotFoundException("토큰을 찾을 수 없습니다. ownerId=" + ownerId));

        // Refresh Token 확인
        if (token.getRefreshToken() == null || token.getRefreshToken().isEmpty()) {
            throw new TokenExpiredException("Refresh Token이 없습니다. 다시 인증이 필요합니다.");
        }

        log.info("Google OAuth2 토큰 갱신 시작 - ownerId: {}", ownerId);

        // 구글 토큰 엔드포인트 URL
        String tokenUrl = "https://oauth2.googleapis.com/token";

        // 요청 헤더 설정 및 바디 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", token.getRefreshToken());
        body.add("grant_type", "refresh_token"); // Refresh Token 방식

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            // POST 요청으로 토큰 갱신
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new IllegalStateException("Google OAuth2 토큰 갱신 응답이 비어있습니다.");
            }

            /**
             * 응답 예시
             * {
             * "access_token": "ya29.a0AfH6SMBx...",
             * "expires_in": 3600,
             * "token_type": "Bearer",
             * "scope": "https://www.googleapis.com/auth/spreadsheets"
             * }
             * 
             * 주의: refresh_token은 갱신 응답에 포함되지 않습니다.
             * 기존 refresh_token을 계속 사용해야 합니다.
             */
            String newAccessToken = (String) responseBody.get("access_token");
            Integer expiresIn = (Integer) responseBody.get("expires_in");

            if (newAccessToken == null) {
                throw new IllegalStateException("Google OAuth2 토큰 갱신 실패: " + responseBody);
            }

            // 만료 시간 계산
            LocalDateTime newExpiryAt = expiresIn != null
                    ? LocalDateTime.now().plusSeconds(expiresIn)
                    : null;

            // 토큰 업데이트 (기존 refresh_token 유지)
            token.updateToken(newAccessToken, token.getRefreshToken(), newExpiryAt);
            tokenRepository.save(token);

            log.info("Google OAuth2 토큰 갱신 성공 - ownerId: {}, expiresIn: {}초", ownerId, expiresIn);

            return newAccessToken;

        } catch (Exception e) {
            log.error("Google OAuth2 토큰 갱신 실패 - ownerId: {}", ownerId, e);

            // Google API 오류인 경우
            if (e.getMessage() != null && e.getMessage().contains("invalid_grant")) {
                throw new TokenExpiredException("Refresh Token이 만료되었거나 유효하지 않습니다. 다시 인증이 필요합니다.");
            }

            throw new IllegalStateException("Google OAuth2 토큰 갱신 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
