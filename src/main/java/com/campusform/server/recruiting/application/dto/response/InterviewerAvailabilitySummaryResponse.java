package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Step2: 시간대별 가능 면접관 수 집계 응답
 * 
 * "전체" 버튼 시각화를 위한 데이터
 * 30분 단위로 해당 시간에 가능한 면접관 수를 제공합니다.
 */
@Schema(description = "시간대별 가능 면접관 수 집계 응답")
@Getter
@RequiredArgsConstructor
public class InterviewerAvailabilitySummaryResponse {

    @Schema(description = "날짜별 집계 목록")
    private final List<DaySummary> summaries;

    public static InterviewerAvailabilitySummaryResponse of(List<DaySummary> summaries) {
        return new InterviewerAvailabilitySummaryResponse(summaries);
    }

    /**
     * 날짜별 집계
     */
    @Schema(description = "날짜별 집계")
    @Getter
    @RequiredArgsConstructor
    public static class DaySummary {

        @Schema(description = "날짜", example = "2024-07-01")
        private final LocalDate date;
        @Schema(description = "해당 날짜의 시간대별 집계 목록")
        private final List<TimeBlockSummary> timeBlocks;

        public static DaySummary of(LocalDate date, List<TimeBlockSummary> timeBlocks) {
            return new DaySummary(date, timeBlocks);
        }
    }

    /**
     * 시간대별 집계 (30분 단위 블록)
     */
    @Schema(description = "시간대별 집계")
    @Getter
    @RequiredArgsConstructor
    public static class TimeBlockSummary {

        @Schema(description = "시작 시간", example = "10:00")
        private final LocalTime startTime;
        @Schema(description = "종료 시간", example = "10:30")
        private final LocalTime endTime; // startTime + 30분
        @Schema(description = "해당 시간 블록에 가능한 면접관 수", example = "2")
        private final int availableInterviewerCount; // 해당 시간 블록에 가능한 면접관 수

        public static TimeBlockSummary of(LocalTime startTime, LocalTime endTime, int availableInterviewerCount) {
            return new TimeBlockSummary(startTime, endTime, availableInterviewerCount);
        }
    }
}
