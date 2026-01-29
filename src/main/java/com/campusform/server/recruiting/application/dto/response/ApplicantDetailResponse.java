package com.campusform.server.recruiting.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
@Getter
@Builder
public class ApplicantDetailResponse {
    // 1. 기본 프로필 정보
    private Long applicantId;
    private String name;
    private String gender;      // MALE, FEMALE
    private String school;
    private String major;
    private String position;    // BACKEND, FRONTEND, DESIGN ...
    private String phoneNumber;
    private String email;
    private String status;      // 합격/보류/불합격 (Stage에 따라 다름)
    private boolean isFavorite; // 즐겨찾기 여부

    // 2. 구글 시트 질의응답 리스트
    private List<AnswerDto> answers;

    @Getter
    @Builder
    public static class AnswerDto {
        private String question;
        private String answer;
    }
}
