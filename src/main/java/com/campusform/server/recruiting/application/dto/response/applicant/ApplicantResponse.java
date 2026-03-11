package com.campusform.server.recruiting.application.dto.response.applicant;

import java.time.LocalDate;
import java.time.LocalTime;
import com.campusform.server.recruiting.application.dto.response.interview.InterviewTimeSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "지원자 목록의 개별 지원자 정보")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantResponse {
    @Schema(description = "지원자 ID", example = "1")
    private Long id;
    @Schema(description = "이름", example = "홍길동")
    private String name;
    @Schema(description = "학교", example = "캠퍼스대학교")
    private String school;
    @Schema(description = "포지션", example = "백엔드")
    private String position;
    @Schema(description = "전공", example = "컴퓨터공학과")
    private String major;
    @Schema(description = "찜하기 여부", example = "true")
    private boolean bookmarked;

    @Schema(description = "해당 단계에서의 지원자 상태 (예: HOLD, PASS, FAIL)", example = "PASS")
    private String status;

    @Schema(description = "해당 단계에서 이 지원자에게 달린 댓글 개수", example = "3")
    private long commentCount;

    /** 면접 단계일 때만 값 존재. 미배정 시 null */
    @Schema(description = "배정된 면접 날짜 (면접 단계일 때만, 미배정 시 null)", example = "2024-07-01")
    private LocalDate interviewDate;
    @Schema(description = "배정된 면접 시작 시간 (면접 단계일 때만, 미배정 시 null)", example = "10:00")
    private LocalTime interviewStartTime;
    @Schema(description = "면접 시간 출처 (MANUAL/AUTO/NONE). 면접 단계일 때만, 미배정 시 null")
    private InterviewTimeSource interviewTimeSource;
}
