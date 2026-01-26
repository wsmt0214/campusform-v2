package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Step1: 면접 정보 설정 조회 응답 DTO
 */
public record InterviewSettingResponse(
        boolean configured,
        LocalDate startDate,
        LocalDate endDate,
        List<LocalDate> interviewDates,
        LocalTime startTime,
        LocalTime endTime,
        Integer maxApplicantsPerSlot,
        Integer minInterviewersPerSlot,
        Integer maxInterviewersPerSlot,
        Integer slotDurationMin,
        Integer slotBreakMin,
        String investigationLinkToken) {

    public static InterviewSettingResponse unconfigured() {
        return new InterviewSettingResponse(false,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
