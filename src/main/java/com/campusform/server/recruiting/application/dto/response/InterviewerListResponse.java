package com.campusform.server.recruiting.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Step2: 면접관 목록 조회 응답
 * 
 * 프로젝트의 모든 관리자(OWNER + ADMIN) 목록
 */
@Schema(description = "면접관 목록 조회 응답")
@Getter
@RequiredArgsConstructor
public class InterviewerListResponse {

    @Schema(description = "면접관(관리자) 목록")
    private final List<AdminInfo> interviewers;

    public static InterviewerListResponse of(List<AdminInfo> interviewers) {
        return new InterviewerListResponse(interviewers);
    }

    /**
     * 관리자 정보 DTO
     */
    @Schema(description = "면접관(관리자) 정보")
    @Getter
    @RequiredArgsConstructor
    public static class AdminInfo {

        @Schema(description = "사용자 ID", example = "101")
        private final Long userId;
        @Schema(description = "닉네임", example = "김면접")
        private final String nickname;
        @Schema(description = "이메일", example = "interviewer@example.com")
        private final String email;
        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        private final String profileImageUrl;

        public static AdminInfo of(Long userId, String nickname, String email, String profileImageUrl) {
            return new AdminInfo(userId, nickname, email, profileImageUrl);
        }
    }
}
