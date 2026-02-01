package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 최종 면접시간 조회 응답 DTO
 */
@Schema(description = "최종 배정된 면접 시간 정보")
@Getter
@RequiredArgsConstructor
public class InterviewAssignedTimeResponse {

    @Schema(description = "지원자 ID", example = "1")
    private final Long applicantId;
    @Schema(description = "지원자 이름", example = "홍길동")
    private final String name;
    @Schema(description = "지원자 학교", example = "캠퍼스대학교")
    private final String school;
    @Schema(description = "지원자 전공", example = "컴퓨터공학과")
    private final String major;
    @Schema(description = "지원 포지션", example = "백엔드")
    private final String position;

    @Schema(description = "배정된 면접 날짜", example = "2024-07-01")
    private final LocalDate interviewDate;
    @Schema(description = "배정된 면접 시작 시간", example = "10:00")
    private final LocalTime startTime;
    @Schema(description = "배정된 면접 종료 시간", example = "10:20")
    private final LocalTime endTime;

    @Schema(description = "면접 시간 출처 (MANUAL: 수동 배정, AUTO: 자동 배정, NONE: 미배정)")
    private final InterviewTimeSource source;

    public static InterviewAssignedTimeResponse of(
            Long applicantId,
            String name,
            String school,
            String major,
            String position,
            LocalDate interviewDate,
            LocalTime startTime,
            LocalTime endTime,
            InterviewTimeSource source) {
        return new InterviewAssignedTimeResponse(
                applicantId,
                name,
                school,
                major,
                position,
                interviewDate,
                startTime,
                endTime,
                source);
    }
}
