package com.campusform.server.recruiting.application.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수동 면접 배정 요청 DTO
 */
@Getter
@NoArgsConstructor // Jackson 역직렬화를 위해 기본 생성자 필요
@AllArgsConstructor
public class AssignManualInterviewRequest {

    // 지원자 ID
    private Long applicantId;

    // 면접 일자
    private LocalDate interviewDate;

    // 시작 시간
    private LocalTime startTime;
}
