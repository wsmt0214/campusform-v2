package com.campusform.server.identity.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 닉네임 수정 응답 DTO
 */
@Schema(description = "닉네임 변경 응답")
public record UpdateNicknameResponse(
        @Schema(description = "변경된 닉네임", example = "새로운별명")
        String nickname
) {
}
