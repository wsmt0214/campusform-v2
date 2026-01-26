package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Step2: 특정 면접관의 가능 시간 조회 응답
 * 
 * 날짜별로 30분 단위 블록 목록을 제공합니다.
 */
@Getter
@RequiredArgsConstructor
public class InterviewerAvailabilityResponse {

    private final Long adminId;
    private final String nickname;
    private final String email;
    private final List<DayAvailability> availabilities;

    public static InterviewerAvailabilityResponse of(Long adminId, String nickname, String email,
            List<DayAvailability> availabilities) {
        return new InterviewerAvailabilityResponse(adminId, nickname, email, availabilities);
    }

    /**
     * 날짜별 가능 시간
     */
    @Getter
    @RequiredArgsConstructor
    public static class DayAvailability {

        private final LocalDate date;
        private final List<TimeBlock> timeBlocks;

        public static DayAvailability of(LocalDate date, List<TimeBlock> timeBlocks) {
            return new DayAvailability(date, timeBlocks);
        }
    }

    /**
     * 30분 단위 시간 블록
     */
    @Getter
    @RequiredArgsConstructor
    public static class TimeBlock {

        private final LocalTime startTime;
        private final LocalTime endTime; // startTime + 30분

        public static TimeBlock of(LocalTime startTime, LocalTime endTime) {
            return new TimeBlock(startTime, endTime);
        }
    }
}
