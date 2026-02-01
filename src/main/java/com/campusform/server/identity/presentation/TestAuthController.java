package com.campusform.server.identity.presentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 테스트용 인증 컨트롤러
 * 
 * Postman 등으로 API 테스트 시 세션을 미리 생성하기 위한 엔드포인트입니다.
 * temporary 프로필에서만 활성화됩니다.
 * 
 * 사용법:
 * 1. POST /api/test/auth/session?userId=1 호출
 * 2. 응답으로 받은 JSESSIONID 쿠키를 Postman에 저장
 * 3. 이후 모든 API 요청에 해당 쿠키 포함
 */
@Profile("temporary")
@Tag(name = "테스트", description = "개발 및 테스트용 API")
@RestController
@RequestMapping("/api/test/auth")
@RequiredArgsConstructor
@Slf4j
public class TestAuthController {

        private final UserRepository userRepository;
        private final AuthService authService;

        /**
         * 특정 userId로 테스트용 세션 생성
         */
        @Operation(summary = "테스트용 세션 생성", description = "지정한 `userId`로 강제 로그인하여 테스트용 세션을 생성합니다. Postman 등에서 API를 테스트할 때 사용합니다.", security = {})
        @PostMapping("/session")
        public ResponseEntity<Map<String, Object>> createTestSession(
                        @Parameter(description = "로그인할 사용자의 ID") @RequestParam Long userId,
                        HttpSession session) {

                // 사용자 조회
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. userId=" + userId));

                // OAuth2User와 동일한 구조의 attributes 생성
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("email", user.getEmail());
                attributes.put("name", user.getNickname());
                attributes.put("picture", user.getProfileImageUrl());
                attributes.put("userId", user.getId()); // CustomOAuth2UserService에서 추가하는 것과 동일

                // OAuth2User 생성 (실제 OAuth2 로그인과 동일한 구조)
                OAuth2User oAuth2User = new DefaultOAuth2User(
                                Collections.singleton(() -> "ROLE_USER"),
                                attributes,
                                "email" // nameAttributeKey
                );

                // OAuth2AuthenticationToken 생성 (실제 OAuth2 로그인과 동일)
                org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken authentication = new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
                                oAuth2User,
                                Collections.singleton(() -> "ROLE_USER"),
                                "google" // registrationId
                );

                // SecurityContext에 Authentication 저장
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);

                // HttpSession에 SecurityContext 저장 (Spring Security가 자동으로 처리하지만 명시적으로 저장)
                session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

                log.info("테스트 세션 생성 완료 - userId: {}, email: {}, sessionId: {}",
                                user.getId(), user.getEmail(), session.getId());

                // 검증: 생성된 세션으로 사용자 정보 조회 테스트
                Authentication savedAuth = SecurityContextHolder.getContext().getAuthentication();
                Long extractedUserId = authService.extractUserId(savedAuth);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "테스트 세션 생성 완료");
                response.put("userId", extractedUserId);
                response.put("email", user.getEmail());
                response.put("nickname", user.getNickname());
                response.put("sessionId", session.getId());
                response.put("instruction", "Postman에서 JSESSIONID 쿠키를 저장하고 이후 요청에 포함하세요.");

                return ResponseEntity.ok(response);
        }

        /**
         * 현재 세션의 인증 정보 확인 (테스트용)
         */
        @Operation(summary = "현재 세션 정보 확인 (테스트용)", description = "현재 요청에 포함된 세션(쿠키)이 유효한지 확인하고, 인증된 사용자 정보를 반환합니다.")
        @PostMapping("/verify")
        public ResponseEntity<Map<String, Object>> verifySession(Authentication authentication) {
                if (authentication == null || !authentication.isAuthenticated()) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("authenticated", false);
                        response.put("message", "세션이 없거나 인증되지 않았습니다.");
                        return ResponseEntity.ok(response);
                }

                Long userId = authService.extractUserId(authentication);
                OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

                Map<String, Object> response = new HashMap<>();
                response.put("authenticated", true);
                response.put("userId", userId);
                response.put("email", oAuth2User.getAttribute("email"));
                response.put("name", oAuth2User.getAttribute("name"));

                return ResponseEntity.ok(response);
        }
}
