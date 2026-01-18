package com.campusform.server.identity.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.dto.response.AuthMeResponse;
import com.campusform.server.identity.application.service.AuthService;

import lombok.RequiredArgsConstructor;

/**
 * 인증 관련 API 컨트롤러
 *
 * 로그아웃은 Spring Security에서 처리 (POST /api/auth/logout)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 세션 유효 확인 (GET /api/auth/me)
     *
     * @param authentication Spring Security Authentication 객체
     * @return 인증 상태 및 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> getCurrentUser(Authentication authentication) {
        AuthMeResponse response = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(response);
    }
}
