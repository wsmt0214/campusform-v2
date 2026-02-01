package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Step2: 특정 면접관의 가능 시간 조회 응답
 * 
 * 날짜별로 30분 단위 블록 목록을 제공합니다.
 */
@Schema(description = "특정 면접관의 가능 시간 조회 응답")
@Getter
@RequiredArgsConstructor
public class InterviewerAvailabilityResponse {

    @Schema(description = "면접관 사용자 ID", example = "101")
    private final Long adminId;
    @Schema(description = "면접관 이름", example = "김면접")
    private final String nickname;
    @Schema(description = "면접관 이메일", example = "interviewer@example.com")
    private final String email;
    @Schema(description = "날짜별 가능 시간 목록")
    private final List<DayAvailability> availabilities;

    public static InterviewerAvailabilityResponse of(Long adminId, String nickname, String email,
            List<DayAvailability> availabilities) {
        return new InterviewerAvailabilityResponse(adminId, nickname, email, availabilities);
    }

    /**
     * 날짜별 가능 시간
     */
    @Schema(description = "날짜별 가능 시간 정보")
    @Getter
    @RequiredArgsConstructor
    public static class DayAvailability {

        @Schema(description = "날짜", example = "2024-07-01")
        private final LocalDate date;
        @Schema(description = "해당 날짜에 가능한 시간 블록 목록")
        private final List<TimeBlock> timeBlocks;

        public static DayAvailability of(LocalDate date, List<TimeBlock> timeBlocks) {
            return new DayAvailability(date, timeBlocks);
        }
    }

    /**
     * 30분 단위 시간 블록
     */
    @Schema(description = "시간 블록 (시작-종료 시간)")
    @Getter
    @RequiredArgsConstructor
    public static class TimeBlock {

        @Schema(description = "시작 시간", example = "10:00")
        private final LocalTime startTime;
        @Schema(description = "종료 시간", example = "10:30")
        private final LocalTime endTime; // startTime + 30분

        public static TimeBlock of(LocalTime startTime, LocalTime endTime) {
            return new TimeBlock(startTime, endTime);
        }
    }
}
