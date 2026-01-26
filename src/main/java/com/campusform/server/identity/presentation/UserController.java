package com.campusform.server.identity.presentation;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.campusform.server.identity.application.dto.request.UpdateNicknameRequest;
import com.campusform.server.identity.application.dto.request.UpdateNotificationSettingRequest;
import com.campusform.server.identity.application.dto.response.DeleteProfileImageResponse;
import com.campusform.server.identity.application.dto.response.NotificationSettingResponse;
import com.campusform.server.identity.application.dto.response.UpdateNicknameResponse;
import com.campusform.server.identity.application.dto.response.UpdateProfileImageResponse;
import com.campusform.server.identity.application.dto.response.UserExistsResponse;
import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.identity.application.service.UserQueryService;
import com.campusform.server.identity.application.service.UserService;
import com.campusform.server.notification.application.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryService userQueryService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuthService authService;

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
    public NotificationSettingResponse getNotificationSetting(Authentication authentication) {
        // userId 추출 책임을 AuthService로 일원화 (컨트롤러에서 Security 세부 구현 숨김)
        Long userId = authService.extractUserId(authentication);
        boolean enabled = notificationService.getNotificationSetting(userId);
        return new NotificationSettingResponse(enabled);
    }

    /**
     * 알림 수신 설정 변경
     */
    @PatchMapping("/notification-setting")
    public NotificationSettingResponse updateNotificationSetting(
            Authentication authentication,
            @RequestBody UpdateNotificationSettingRequest request) {
        Long userId = authService.extractUserId(authentication);
        boolean enabled = notificationService.updateNotificationSetting(userId, request.enabled());
        return new NotificationSettingResponse(enabled);
    }

    /**
     * 프로필 이미지 업데이트
     */
    @PatchMapping("/profile-image")
    public UpdateProfileImageResponse updateProfileImage(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestParam("image") MultipartFile image) {
        Long userId = oauth2User.getAttribute("userId");
        String profileImageUrl = userService.updateProfileImage(userId, image);
        return new UpdateProfileImageResponse(profileImageUrl);
    }

    /**
     * 프로필 이미지 삭제
     */
    @DeleteMapping("/profile-image")
    public DeleteProfileImageResponse deleteProfileImage(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        Long userId = oauth2User.getAttribute("userId");
        userService.deleteProfileImage(userId);
        return DeleteProfileImageResponse.success();
    }

    /**
     * 닉네임 수정
     */
    @PatchMapping("/nickname")
    public UpdateNicknameResponse updateNickname(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestBody UpdateNicknameRequest request) {
        Long userId = oauth2User.getAttribute("userId");
        String nickname = userService.updateNickname(userId, request.nickname());
        return new UpdateNicknameResponse(nickname);
    }
}
