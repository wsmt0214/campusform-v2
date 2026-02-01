package com.campusform.server.identity.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.dto.response.AuthMeResponse;
import com.campusform.server.identity.application.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 인증 관련 API 컨트롤러
 *
 * 로그아웃은 Spring Security에서 처리 (POST /api/auth/logout)
 */
@Tag(name = "인증", description = "로그인/로그아웃 및 인증 상태 관련 API")
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
    @Operation(summary = "현재 로그인된 사용자 정보 확인", description = "현재 세션의 유효성을 검사하고, 로그인된 사용자의 기본 정보를 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> getCurrentUser(Authentication authentication) {
        AuthMeResponse response = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(response);
    }
}
