package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Step2: 시간대별 가능 면접관 수 집계 응답
 * 
 * "전체" 버튼 시각화를 위한 데이터
 * 30분 단위로 해당 시간에 가능한 면접관 수를 제공합니다.
 */
@Getter
@RequiredArgsConstructor
public class InterviewerAvailabilitySummaryResponse {

    private final List<DaySummary> summaries;

    public static InterviewerAvailabilitySummaryResponse of(List<DaySummary> summaries) {
        return new InterviewerAvailabilitySummaryResponse(summaries);
    }

    /**
     * 날짜별 집계
     */
    @Getter
    @RequiredArgsConstructor
    public static class DaySummary {

        private final LocalDate date;
        private final List<TimeBlockSummary> timeBlocks;

        public static DaySummary of(LocalDate date, List<TimeBlockSummary> timeBlocks) {
            return new DaySummary(date, timeBlocks);
        }
    }

    /**
     * 시간대별 집계 (30분 단위 블록)
     */
    @Getter
    @RequiredArgsConstructor
    public static class TimeBlockSummary {

        private final LocalTime startTime;
        private final LocalTime endTime; // startTime + 30분
        private final int availableInterviewerCount; // 해당 시간 블록에 가능한 면접관 수

        public static TimeBlockSummary of(LocalTime startTime, LocalTime endTime, int availableInterviewerCount) {
            return new TimeBlockSummary(startTime, endTime, availableInterviewerCount);
        }
    }
}
