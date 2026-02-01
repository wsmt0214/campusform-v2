package com.campusform.server.identity.presentation;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.dto.request.UpdateNotificationSettingRequest;
import com.campusform.server.identity.application.dto.response.NotificationSettingResponse;
import com.campusform.server.identity.application.dto.response.UserExistsResponse;
import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.identity.application.service.UserQueryService;
import com.campusform.server.notification.application.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "사용자", description = "사용자 정보 조회 및 설정 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryService userQueryService;
    // private final UserService userService;
    private final NotificationService notificationService;
    private final AuthService authService;

    /**
     * 이메일로 회원 존재 여부 확인
     */
    @Operation(summary = "이메일 가입 여부 확인", description = "지정된 이메일로 가입한 사용자가 있는지 확인합니다.", security = {})
    @GetMapping("/exists")
    public UserExistsResponse existsByEmail(@Parameter(description = "확인할 사용자 이메일") @RequestParam String email) {
        return userQueryService.findByEmail(email);
    }

    /**
     * 알림 수신 설정 조회
     */
    @Operation(summary = "알림 수신 설정 조회", description = "현재 로그인한 사용자의 이메일/SMS 알림 수신 동의 상태를 조회합니다.")
    @GetMapping("/notification-setting")
    public NotificationSettingResponse getNotificationSetting(Authentication authentication) {
        // userId 추출 책임을 AuthService로 일원화 (컨트롤러에서 Security 세부 구현 숨김)
        Long userId = authService.extractUserId(authentication);
        boolean enabled = notificationService.getNotificationSetting(userId);
        return new NotificationSettingResponse(enabled);
    }

    /**
     * 알림 수신 설정 변경
     */
    @Operation(summary = "알림 수신 설정 변경", description = "현재 로그인한 사용자의 이메일/SMS 알림 수신 동의 상태를 변경합니다.")
    @PatchMapping("/notification-setting")
    public NotificationSettingResponse updateNotificationSetting(
            Authentication authentication,
            @RequestBody UpdateNotificationSettingRequest request) {
        Long userId = authService.extractUserId(authentication);
        boolean enabled = notificationService.updateNotificationSetting(userId, request.enabled());
        return new NotificationSettingResponse(enabled);
    }
}
