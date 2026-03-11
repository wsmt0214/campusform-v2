package com.campusform.server.recruiting.application.dto.response.interview;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 면접 슬롯 목록 조회 응답 DTO
 */
@Schema(description = "면접 슬롯 목록 응답")
@Getter
@RequiredArgsConstructor
public class InterviewSlotListResponse {

    @Schema(description = "날짜별 슬롯 요약 목록")
    private final List<DaySlotSummary> summaries;

    public static InterviewSlotListResponse of(List<DaySlotSummary> summaries) {
        return new InterviewSlotListResponse(summaries);
    }

    /**
     * 날짜별 슬롯 요약
     */
    @Schema(description = "날짜별 슬롯 요약")
    @Getter
    @RequiredArgsConstructor
    public static class DaySlotSummary {

        @Schema(description = "날짜", example = "2024-07-01")
        private final LocalDate date;
        @Schema(description = "해당 날짜의 슬롯 목록")
        private final List<SlotInfo> slots;

        public static DaySlotSummary of(LocalDate date, List<SlotInfo> slots) {
            return new DaySlotSummary(date, slots);
        }
    }

    /**
     * 슬롯 정보
     */
    @Schema(description = "개별 슬롯 정보")
    @Getter
    @RequiredArgsConstructor
    public static class SlotInfo {

        @Schema(description = "슬롯 시작 시간", example = "10:00")
        private final LocalTime startTime;
        @Schema(description = "슬롯 종료 시간", example = "10:20")
        private final LocalTime endTime;
        @Schema(description = "해당 슬롯에 참여 가능한 면접관 수", example = "2")
        private final int availableInterviewerCount;

        public static SlotInfo of(LocalTime startTime, LocalTime endTime, int availableInterviewerCount) {
            return new SlotInfo(startTime, endTime, availableInterviewerCount);
        }
    }
}
