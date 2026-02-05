package com.campusform.server.project.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 목록 응답 DTO
 */
@Schema(description = "관리자 목록 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminListResponse {

    @Schema(description = "관리자 목록")
    private List<AdminInfo> admins;

    @Schema(description = "관리자 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminInfo {
        @Schema(description = "관리자 ID", example = "1")
        private Long adminId;

        @Schema(description = "관리자 이름", example = "홍길동")
        private String adminName;

        @Schema(description = "관리자 이메일", example = "admin@example.com")
        private String email;

        @Schema(description = "관리자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
        private String profileImageUrl;

        @Schema(description = "관리자 역할 (OWNER 또는 ADMIN)", example = "OWNER")
        private String role;
    }
}
