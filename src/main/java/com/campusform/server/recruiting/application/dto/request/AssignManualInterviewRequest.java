package com.campusform.server.recruiting.application.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수동 면접 배정 요청 DTO
 */
@Schema(description = "수동 면접 배정 요청")
@Getter
@NoArgsConstructor // Jackson 역직렬화를 위해 기본 생성자 필요
@AllArgsConstructor
public class AssignManualInterviewRequest {

    @Schema(description = "면접을 배정할 지원자 ID", example = "10")
    private Long applicantId;

    @Schema(description = "면접 일자", example = "2024-07-01")
    private LocalDate interviewDate;

    @Schema(description = "면접 시작 시간", example = "14:00")
    private LocalTime startTime;
}
