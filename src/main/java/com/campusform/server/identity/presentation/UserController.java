package com.campusform.server.identity.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import com.campusform.server.global.security.CurrentUserId;
import com.campusform.server.identity.application.dto.request.UpdateNotificationSettingRequest;
import com.campusform.server.identity.application.dto.response.NotificationSettingResponse;
import com.campusform.server.identity.application.dto.response.UserExistsResponse;
import com.campusform.server.identity.application.service.UserQueryService;
import com.campusform.server.identity.application.service.UserWithdrawalService;
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
    private final NotificationService notificationService;
    private final UserWithdrawalService userWithdrawalService;

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
    public NotificationSettingResponse getNotificationSetting(@CurrentUserId Long userId) {
        boolean enabled = notificationService.getNotificationSetting(userId);
        return new NotificationSettingResponse(enabled);
    }

    /**
     * 알림 수신 설정 변경
     */
    @Operation(summary = "알림 수신 설정 변경", description = "현재 로그인한 사용자의 이메일/SMS 알림 수신 동의 상태를 변경합니다.")
    @PatchMapping("/notification-setting")
    public NotificationSettingResponse updateNotificationSetting(
            @CurrentUserId Long userId,
            @RequestBody UpdateNotificationSettingRequest request) {
        boolean enabled = notificationService.updateNotificationSetting(userId, request.enabled());
        return new NotificationSettingResponse(enabled);
    }

    /**
     * 회원 탈퇴 (소유 프로젝트 삭제·공동 관리자 해제 후 사용자 및 연동 정보 삭제, 세션 무효화)
     */
    @Operation(summary = "회원 탈퇴", description = "현재 계정을 삭제합니다. 본인이 소유한 프로젝트와 관련 데이터가 함께 삭제되며, 다른 프로젝트의 공동 관리자인 경우 해당 프로젝트에서만 제외됩니다.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@CurrentUserId Long userId, HttpServletRequest request) {
        userWithdrawalService.withdraw(userId);
        request.getSession().invalidate();
        return ResponseEntity.noContent().build();
    }
}
