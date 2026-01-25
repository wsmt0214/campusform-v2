package com.campusform.server.identity.presentation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.dto.request.UpdateNotificationSettingRequest;
import com.campusform.server.identity.application.dto.response.NotificationSettingResponse;
import com.campusform.server.identity.application.dto.response.UserExistsResponse;
import com.campusform.server.identity.application.service.UserQueryService;
import com.campusform.server.notification.application.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryService userQueryService;
    private final NotificationService notificationService;

    /**
     * 이메일로 회원 존재 여부 확인
     */
    @GetMapping("/exists")
    public UserExistsResponse existsByEmail(@RequestParam String email) {
        return userQueryService.findByEmail(email);
    }

    /**
     * 알림 수신 설정 조회
     */
    @GetMapping("/notification-setting")
    public NotificationSettingResponse getNotificationSetting(@AuthenticationPrincipal OAuth2User oauth2User) {
        Long userId = oauth2User.getAttribute("userId");
        boolean enabled = notificationService.getNotificationSetting(userId);
        return new NotificationSettingResponse(enabled);
    }

    /**
     * 알림 수신 설정 변경
     */
    @PatchMapping("/notification-setting")
    public NotificationSettingResponse updateNotificationSetting(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestBody UpdateNotificationSettingRequest request) {
        Long userId = oauth2User.getAttribute("userId");
        boolean enabled = notificationService.updateNotificationSetting(userId, request.enabled());
        return new NotificationSettingResponse(enabled);
    }
}
