package com.campusform.server.identity.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 존재 여부 확인 응답 DTO
 * 
 * 이메일로 사용자 존재 여부를 확인한 결과를 담는 객체입니다.
 */
@Schema(description = "이메일 가입 여부 확인 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserExistsResponse {
    @Schema(description = "존재 여부", example = "true")
    private boolean exists;
    @Schema(description = "사용자 ID (존재할 경우)", example = "1")
    private Long userId;
    @Schema(description = "닉네임 (존재할 경우)", example = "캠퍼스폼")
    private String nickname;
    @Schema(description = "검색한 이메일", example = "user@example.com")
    private String email;
    @Schema(description = "프로필 이미지 URL (존재할 경우)", example = "https://example.com/profile.jpg")
    private String profileImageUrl;

    public static UserExistsResponse found(Long userId, String nickname, String email, String profileImageUrl) {
        return new UserExistsResponse(true, userId, nickname, email, profileImageUrl);
    }

    public static UserExistsResponse notFound(String email) {
        return new UserExistsResponse(false, null, null, email, null);
    }
}
