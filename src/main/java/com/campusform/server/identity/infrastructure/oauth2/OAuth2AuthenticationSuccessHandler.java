package com.campusform.server.identity.infrastructure.oauth2;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * OAuth2 로그인 성공 시 처리하는 핸들러
 *
 * 세션 생성 후 프론트엔드로 리다이렉트
 * redirect_uri 쿠키가 있고 화이트리스트에 포함되면 해당 URI로 리다이렉트
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final CookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Value("${app.oauth2.login-redirect-uri}")
    private String defaultRedirectUri;

    @Value("${app.oauth2.allowed-redirect-uris:}")
    private String allowedRedirectUrisString;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request);
        cookieAuthorizationRequestRepository.removeRedirectUriCookie(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String determineTargetUrl(HttpServletRequest request) {
        return cookieAuthorizationRequestRepository.getRedirectUriCookieValue(request)
                .filter(this::isAuthorizedRedirectUri)
                .orElse(defaultRedirectUri);
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        return Arrays.stream(allowedRedirectUrisString.split(","))
                .map(String::trim)
                .anyMatch(uri::startsWith);
    }
}