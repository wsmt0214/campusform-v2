package com.campusform.server.identity.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프로필 이미지 업데이트 응답 DTO
 */
@Schema(description = "프로필 이미지 변경 응답")
public record UpdateProfileImageResponse(
        @Schema(description = "새로운 프로필 이미지 URL", example = "https://example.com/new_profile.jpg")
        String profileImageUrl
) {
}
