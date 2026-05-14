package com.campusform.server.identity.application.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.global.exception.UnauthorizedException;
import com.campusform.server.identity.application.dto.response.AuthMeResponse;
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
                .map(user -> AuthMeResponse.authenticated(
                        user.getId(),
                        user.getEmail(),
                        user.getNickname(),
                        user.getProfileImageUrl(),
                        user.isOnboarded()))
                .orElseGet(AuthMeResponse::unauthenticated);
    }

    /**
     * Authentication에서 현재 사용자 ID 추출
     */
    public Long extractUserId(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            // 인증이 없는 요청(로그인 필요)은 401로 내려주는 게 REST 관점에서 자연스럽습니다.
            throw new UnauthorizedException("인증이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User)) {
            // 예상할 수 없는 인증 객체에 대해서도 401 반환
            throw new UnauthorizedException("유효하지 않은 인증 방식입니다.");
        }

        OAuth2User oAuth2User = (OAuth2User) principal;
        Object userIdObj = oAuth2User.getAttribute("userId");

        if (userIdObj == null) {
            // 정상 인증 흐름이라면 세션(principal)에 userId가 반드시 들어있어야 합니다.
            // 누락되면 인증 컨텍스트가 깨진 상태이므로 401로 처리합니다.
            throw new UnauthorizedException("사용자 ID를 찾을 수 없습니다.");
        }

        // Long으로 변환 (Integer일 수도 있으므로 처리)
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        } else if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Number) {
            return ((Number) userIdObj).longValue();
        } else {
            throw new UnauthorizedException("유효하지 않은 사용자 ID 형식입니다.");
        }
    }

    /**
     * 사용자 인증 여부 확인
     */
    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
