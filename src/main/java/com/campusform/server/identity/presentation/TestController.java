package com.campusform.server.identity.presentation;

import org.springframework.context.annotation.Profile;
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
import com.campusform.server.identity.application.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * 로컬 테스트용 컨트롤러 (temporary 프로파일에서만 활성화)
 *
 * 프로덕션 배포 시 이 파일은 삭제하거나 @Profile("temporary")로 제한됩니다.
 */
@Profile("temporary")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final UserService userService;

    /**
     * 로컬 테스트용 프로필 이미지 업데이트 (인증 불필요)
     *
     * @param userId 테스트할 사용자 ID
     * @param image 업로드할 이미지
     */
    @PatchMapping("/profile-image")
    public UpdateProfileImageResponse testUpdateProfileImage(
            @RequestParam Long userId,
            @RequestParam("image") MultipartFile image) {
        String profileImageUrl = userService.updateProfileImage(userId, image);
        return new UpdateProfileImageResponse(profileImageUrl);
    }

    /**
     * 로컬 테스트용 프로필 이미지 삭제 (인증 불필요)
     *
     * @param userId 테스트할 사용자 ID
     */
    @DeleteMapping("/profile-image")
    public DeleteProfileImageResponse testDeleteProfileImage(
            @RequestParam Long userId) {
        userService.deleteProfileImage(userId);
        return DeleteProfileImageResponse.success();
    }

    /**
     * 로컬 테스트용 닉네임 수정 (인증 불필요)
     *
     * @param userId 테스트할 사용자 ID
     * @param request 닉네임 수정 요청
     */
    @PatchMapping("/nickname")
    public UpdateNicknameResponse testUpdateNickname(
            @RequestParam Long userId,
            @RequestBody UpdateNicknameRequest request) {
        String nickname = userService.updateNickname(userId, request.nickname());
        return new UpdateNicknameResponse(nickname);
    }
}
