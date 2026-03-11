package com.campusform.server.recruiting.application.dto.response.interview;

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

    @Schema(description = "배정된 면접 날짜", example = "2024-07-01")
    private final LocalDate interviewDate;
    @Schema(description = "배정된 면접 시작 시간", example = "10:00")
    private final LocalTime startTime;
    // 종료 시간은 클라이언트에서 slotDuration 등 설정값으로 계산 가능하므로 응답에서 제외합니다.

    @Schema(description = "면접 시간 출처 (MANUAL: 수동 배정, AUTO: 자동 배정, NONE: 미배정)")
    private final InterviewTimeSource source;

    public static InterviewAssignedTimeResponse of(
            Long applicantId,
            LocalDate interviewDate,
            LocalTime startTime,
            InterviewTimeSource source) {
        return new InterviewAssignedTimeResponse(
                applicantId,
                interviewDate,
                startTime,
                source);
    }
}
