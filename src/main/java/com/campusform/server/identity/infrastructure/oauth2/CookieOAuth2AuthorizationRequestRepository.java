package com.campusform.server.identity.infrastructure.oauth2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 인증 요청을 쿠키에 저장하는 저장소
 *
 * 서브도메인 간 세션 공유 문제를 해결하기 위해 쿠키 기반 저장소 사용
 * 세션 대신 쿠키에 인증 요청을 저장하여 api.campus-form-server.kro.kr와
 * web.campus-form-server.kro.kr 간 공유 가능
 */
@Slf4j
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    private static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180; // 3분

    @Value("${COOKIE_DOMAIN:campus-form-server.kro.kr}")
    private String cookieDomain;

    @Value("${COOKIE_SECURE:true}")
    private boolean cookieSecure;

    @Value("${COOKIE_SAME_SITE:none}")
    private String cookieSameSite;

    /**
     * 쿠키에서 OAuth2 인증 요청 로드
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        java.util.Optional<Cookie> cookie = getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        log.info("[OAuth2 Cookie] 로드 - 쿠키 존재: {}, 전체 쿠키 수: {}",
                cookie.isPresent(), request.getCookies() != null ? request.getCookies().length : 0);
        return cookie
                .map(c -> deserialize(c, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    /**
     * OAuth2 인증 요청을 쿠키에 저장
     */
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            // 인증 요청이 null이면 쿠키 삭제
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        // 인증 요청을 쿠키에 저장
        String serialized = serialize(authorizationRequest);
        log.info("[OAuth2 Cookie] 저장 - 쿠키 크기: {} bytes, secure: {}, sameSite: {}, domain: '{}'",
                serialized.length(), cookieSecure, cookieSameSite, cookieDomain);
        addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                serialized, COOKIE_EXPIRE_SECONDS);

        // 리다이렉트 URI도 쿠키에 저장 (필요한 경우)
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        log.info("[OAuth2 Cookie] redirect_uri 파라미터: '{}'", redirectUriAfterLogin);
        if (redirectUriAfterLogin != null && !redirectUriAfterLogin.isEmpty()) {
            addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin,
                    COOKIE_EXPIRE_SECONDS);
        }
    }

    /**
     * OAuth2 인증 요청 제거
     */
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
            HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        }
        return authorizationRequest;
    }

    /**
     * redirect_uri 쿠키 값 조회
     */
    public java.util.Optional<String> getRedirectUriCookieValue(HttpServletRequest request) {
        java.util.Optional<String> value = getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);
        log.info("[OAuth2 Cookie] redirect_uri 쿠키 조회: {}", value.orElse("없음"));
        return value;
    }

    /**
     * redirect_uri 쿠키 삭제
     */
    public void removeRedirectUriCookie(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }

    /**
     * 쿠키 추가
     */
    private void addCookie(HttpServletResponse response, String name, String value,
            int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(cookieSecure);
        cookie.setAttribute("SameSite", cookieSameSite);
        // 쿠키 도메인 설정: 서브도메인 간 공유를 위해 점(.)으로 시작
        // api.campus-form-server.kro.kr와 web.campus-form-server.kro.kr 간 공유
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        response.addCookie(cookie);
    }

    /**
     * 쿠키 삭제
     */
    private void deleteCookie(HttpServletRequest request, HttpServletResponse response,
            String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    cookie.setHttpOnly(true);
                    cookie.setSecure(cookieSecure);
                    cookie.setAttribute("SameSite", cookieSameSite);
                    // 도메인도 일치시켜야 삭제됨
                    if (cookieDomain != null && !cookieDomain.isEmpty()) {
                        cookie.setDomain(cookieDomain);
                    }
                    response.addCookie(cookie);
                }
            }
        }
    }

    /**
     * 쿠키 가져오기
     */
    private java.util.Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return java.util.Optional.of(cookie);
                }
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * 객체를 Base64 문자열로 직렬화
     *
     * Java 표준 직렬화를 사용하여 객체를 바이트 배열로 변환한 후 Base64로 인코딩
     *
     * @param object 직렬화할 객체
     * @return Base64로 인코딩된 문자열
     * @throws RuntimeException 직렬화 실패 시
     */
    private String serialize(Object object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            oos.flush();
            return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    /**
     * Base64 문자열을 객체로 역직렬화
     *
     * Base64 디코딩 후 Java 표준 역직렬화를 사용하여 객체로 변환
     *
     * @param cookie 역직렬화할 쿠키
     * @param clazz  반환할 객체 타입
     * @return 역직렬화된 객체
     * @param <T> 객체 타입
     * @throws RuntimeException 역직렬화 실패 시
     */
    private <T> T deserialize(Cookie cookie, Class<T> clazz) {
        try {
            byte[] data = Base64.getUrlDecoder().decode(cookie.getValue());
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    ObjectInputStream ois = new ObjectInputStream(bais)) {
                Object obj = ois.readObject();
                return clazz.cast(obj);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize object", e);
        }
    }
}