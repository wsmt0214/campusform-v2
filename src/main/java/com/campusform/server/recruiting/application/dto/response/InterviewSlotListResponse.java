package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 면접 슬롯 목록 조회 응답 DTO
 */
@Getter
@RequiredArgsConstructor
public class InterviewSlotListResponse {

    private final List<DaySlotSummary> summaries;

    public static InterviewSlotListResponse of(List<DaySlotSummary> summaries) {
        return new InterviewSlotListResponse(summaries);
    }

    /**
     * 날짜별 슬롯 요약
     */
    @Getter
    @RequiredArgsConstructor
    public static class DaySlotSummary {

        private final LocalDate date;
        private final List<SlotInfo> slots;

        public static DaySlotSummary of(LocalDate date, List<SlotInfo> slots) {
            return new DaySlotSummary(date, slots);
        }
    }

    /**
     * 슬롯 정보
     */
    @Getter
    @RequiredArgsConstructor
    public static class SlotInfo {

        private final LocalTime startTime;
        private final LocalTime endTime;
        private final int availableInterviewerCount;

        public static SlotInfo of(LocalTime startTime, LocalTime endTime, int availableInterviewerCount) {
            return new SlotInfo(startTime, endTime, availableInterviewerCount);
        }
    }
}
