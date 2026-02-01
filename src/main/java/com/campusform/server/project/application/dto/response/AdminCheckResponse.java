package com.campusform.server.project.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 확인 응답 DTO
 * 
 * 이메일로 관리자 존재 여부를 확인한 결과를 담는 객체입니다.
 */
@Schema(description = "이메일로 관리자(사용자) 조회 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminCheckResponse {

    @Schema(description = "존재 여부", example = "true")
    private boolean exists;
    @Schema(description = "사용자 ID (존재할 경우)", example = "2")
    private Long userId;
    @Schema(description = "닉네임 (존재할 경우)", example = "김관리")
    private String nickname;
    @Schema(description = "검색한 이메일", example = "admin@example.com")
    private String email;
    @Schema(description = "프로필 이미지 URL (존재할 경우)", example = "https://example.com/profile.jpg")
    private String profileImageUrl;
    @Schema(description = "에러 메시지 (사용자가 없을 경우)", example = "해당 이메일로 가입된 회원이 없습니다: ...")
    private String errorMessage;

    public static AdminCheckResponse success(Long userId, String nickname, String email, String profileImageUrl) {
        return new AdminCheckResponse(true, userId, nickname, email, profileImageUrl, null);
    }

    public static AdminCheckResponse notFound(String email) {
        return new AdminCheckResponse(false, null, null, null, email,
                "해당 이메일로 가입된 회원이 없습니다: " + email);
    }
}
