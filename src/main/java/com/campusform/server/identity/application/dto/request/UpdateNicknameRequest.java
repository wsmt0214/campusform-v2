package com.campusform.server.identity.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 닉네임 수정 요청 DTO
 */
@Schema(description = "닉네임 변경 요청")
public record UpdateNicknameRequest(
        @Schema(description = "새로운 닉네임", example = "새로운별명")
        String nickname
) {
}
