package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 최종 면접시간 조회 응답 DTO
 */
@Getter
@RequiredArgsConstructor
public class InterviewAssignedTimeResponse {

    private final Long applicantId;
    private final String name;
    private final String school;
    private final String major;
    private final String position;

    private final LocalDate interviewDate;
    private final LocalTime startTime;
    private final LocalTime endTime;

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
