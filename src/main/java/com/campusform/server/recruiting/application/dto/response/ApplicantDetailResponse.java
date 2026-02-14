package com.campusform.server.recruiting.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "지원자 상세 정보 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantDetailResponse {
    @Schema(description = "지원자 ID", example = "1")
    private Long applicantId;
    @Schema(description = "이름", example = "홍길동")
    private String name;
    @Schema(description = "성별", example = "MALE")
    private String gender;
    @Schema(description = "학교", example = "캠퍼스대학교")
    private String school;
    @Schema(description = "전공", example = "컴퓨터공학과")
    private String major;
    @Schema(description = "지원 포지션", example = "백엔드")
    private String position;
    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phoneNumber;
    @Schema(description = "이메일", example = "applicant@example.com")
    private String email;
    @Schema(description = "현재 단계에서의 상태 (e.g., PASS, FAIL, PENDING)", example = "PASS")
    private String status;
    @Schema(description = "찜하기(즐겨찾기) 여부", example = "true")
    private boolean isFavorite;
    @Schema(description = "해당 단계에서 이 지원자에게 달린 댓글 개수", example = "3")
    private long commentCount;

    @Schema(description = "Google Sheet의 질문/답변 목록")
    private List<AnswerDto> answers;

    @Schema(description = "질문/답변")
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDto {
        @Schema(description = "질문 (Google Sheet의 헤더)", example = "자기소개를 해주세요.")
        private String question;
        @Schema(description = "답변", example = "안녕하세요, 저는...")
        private String answer;
    }
}
