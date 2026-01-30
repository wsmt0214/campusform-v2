package com.campusform.server.identity.presentation;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.campusform.server.identity.application.dto.request.UpdateNicknameRequest;
import com.campusform.server.identity.application.dto.response.DeleteProfileImageResponse;
import com.campusform.server.identity.application.dto.response.UpdateNicknameResponse;
import com.campusform.server.identity.application.dto.response.UpdateProfileImageResponse;
import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.identity.application.service.UserService;

import lombok.RequiredArgsConstructor;

@Profile("!temporary")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * 프로필 이미지 업데이트
     */
    @PatchMapping("/profile-image")
    public UpdateProfileImageResponse updateProfileImage(
            Authentication authentication,
            @RequestParam("image") MultipartFile image) {
        Long userId = authService.extractUserId(authentication);
        String profileImageUrl = userService.updateProfileImage(userId, image);
        return new UpdateProfileImageResponse(profileImageUrl);
    }

    /**
     * 프로필 이미지 삭제
     */
    @DeleteMapping("/profile-image")
    public DeleteProfileImageResponse deleteProfileImage(
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        userService.deleteProfileImage(userId);
        return DeleteProfileImageResponse.success();
    }

    /**
     * 닉네임 수정
     */
    @PatchMapping("/nickname")
    public UpdateNicknameResponse updateNickname(
            Authentication authentication,
            @RequestBody UpdateNicknameRequest request) {
        Long userId = authService.extractUserId(authentication);
        String nickname = userService.updateNickname(userId, request.nickname());
        return new UpdateNicknameResponse(nickname);
    }
}
