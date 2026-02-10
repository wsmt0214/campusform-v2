package com.campusform.server.identity.infrastructure.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * OAuth2 로그인 실패 시 처리하는 핸들러
 *
 * 에러 메시지와 함께 프론트엔드로 리다이렉트
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final CookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Value("${app.oauth2.login-redirect-uri}")
    private String defaultRedirectUri;

    @Value("${app.oauth2.allowed-redirect-uris:}")
    private String allowedRedirectUrisString;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request);
        cookieAuthorizationRequestRepository.removeRedirectUriCookie(request, response);

        String errorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, targetUrl + "?error=" + errorMessage);
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