package com.campusform.server.identity.presentation;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.campusform.server.identity.application.dto.request.UpdateNicknameRequest;
import com.campusform.server.identity.application.dto.response.DeleteProfileImageResponse;
import com.campusform.server.identity.application.dto.response.UpdateNicknameResponse;
import com.campusform.server.identity.application.dto.response.UpdateProfileImageResponse;
import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.identity.application.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 프로필 수정 컨트롤러
 * S3 파일 업로드가 필요하므로 로컬 환경에서는 비활성화
 */
@Profile("!local")
@Tag(name = "사용자", description = "사용자 정보 조회 및 설정 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * 프로필 이미지 업데이트
     */
    @Operation(summary = "프로필 이미지 변경", description = "현재 로그인한 사용자의 프로필 이미지를 변경합니다.")
    @PatchMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UpdateProfileImageResponse updateProfileImage(
            Authentication authentication,
            @Parameter(description = "업로드할 이미지 파일", schema = @Schema(type = "string", format = "binary"))
            @RequestPart("image") MultipartFile image) {
        Long userId = authService.extractUserId(authentication);
        String profileImageUrl = userService.updateProfileImage(userId, image);
        return new UpdateProfileImageResponse(profileImageUrl);
    }

    /**
     * 프로필 이미지 삭제
     */
    @Operation(summary = "프로필 이미지 삭제", description = "현재 로그인한 사용자의 프로필 이미지를 기본 이미지로 초기화합니다.")
    @DeleteMapping("/profile-image")
    public DeleteProfileImageResponse deleteProfileImage(Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        userService.deleteProfileImage(userId);
        return DeleteProfileImageResponse.success();
    }

    /**
     * 닉네임 변경
     */
    @Operation(summary = "닉네임 변경", description = "현재 로그인한 사용자의 닉네임을 변경합니다.")
    @PatchMapping("/nickname")
    public UpdateNicknameResponse updateNickname(
            Authentication authentication,
            @RequestBody UpdateNicknameRequest request) {
        Long userId = authService.extractUserId(authentication);
        String nickname = userService.updateNickname(userId, request.nickname());
        return new UpdateNicknameResponse(nickname);
    }
}
