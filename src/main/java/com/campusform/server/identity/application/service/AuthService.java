package com.campusform.server.identity.application.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.identity.application.dto.response.AuthMeResponse;
import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;

    /**
     * 현재 로그인한 사용자 정보 조회 (세션 유효 확인)
     *
     * @param authentication Spring Security Authentication 객체
     * @return 인증 상태 및 사용자 정보
     */
    public AuthMeResponse getCurrentUser(Authentication authentication) {
        if (!isAuthenticated(authentication))
            return AuthMeResponse.unauthenticated();

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        return userRepository.findByEmail(email)
                .map(user -> AuthMeResponse.authenticated(user.getId(), user.getEmail(), user.getNickname()))
                .orElseGet(AuthMeResponse::unauthenticated);
    }

    /**
     * 사용자 인증 여부 확인
     */
    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
