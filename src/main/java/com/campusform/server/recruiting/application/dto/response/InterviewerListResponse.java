package com.campusform.server.recruiting.application.dto.response;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Step2: 면접관 목록 조회 응답
 * 
 * 프로젝트의 모든 관리자(OWNER + ADMIN) 목록
 */
@Getter
@RequiredArgsConstructor
public class InterviewerListResponse {

    private final List<AdminInfo> interviewers;

    public static InterviewerListResponse of(List<AdminInfo> interviewers) {
        return new InterviewerListResponse(interviewers);
    }

    /**
     * 관리자 정보 DTO
     */
    @Getter
    @RequiredArgsConstructor
    public static class AdminInfo {

        private final Long userId;
        private final String nickname;
        private final String email;
        private final String profileImageUrl;

        public static AdminInfo of(Long userId, String nickname, String email, String profileImageUrl) {
            return new AdminInfo(userId, nickname, email, profileImageUrl);
        }
    }
}
