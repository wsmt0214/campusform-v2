package com.campusform.server.identity.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프로필 이미지 삭제 응답 DTO
 */
@Schema(description = "프로필 이미지 삭제 응답")
public record DeleteProfileImageResponse(
        @Schema(description = "응답 메시지", example = "프로필 이미지가 삭제되었습니다.")
        String message
) {
    public static DeleteProfileImageResponse success() {
        return new DeleteProfileImageResponse("프로필 이미지가 삭제되었습니다.");
    }
}
