package com.campusform.server.recruiting.application.dto.response.message;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 모든 슬롯별 지원자 목록 조회 응답 DTO
 * 
 * 날짜별 -> 슬롯별 -> 지원자 목록 구조로 제공됩니다.
 */
@Schema(description = "슬롯별 신청 지원자 목록 응답")
@Getter
@RequiredArgsConstructor
public class SlotApplicantListResponse {

    @Schema(description = "날짜별 슬롯 요약 목록")
    private final List<DaySlotSummary> summaries;

    public static SlotApplicantListResponse of(List<DaySlotSummary> summaries) {
        return new SlotApplicantListResponse(summaries);
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
        @Schema(description = "해당 날짜의 슬롯별 지원자 정보 목록")
        private final List<SlotApplicantInfo> slots;

        public static DaySlotSummary of(LocalDate date, List<SlotApplicantInfo> slots) {
            return new DaySlotSummary(date, slots);
        }
    }

    /**
     * 슬롯별 지원자 정보
     */
    @Schema(description = "슬롯별 지원자 정보")
    @Getter
    @RequiredArgsConstructor
    public static class SlotApplicantInfo {

        @Schema(description = "슬롯 시작 시간", example = "10:00")
        private final LocalTime startTime;
        @Schema(description = "슬롯 종료 시간", example = "10:20")
        private final LocalTime endTime;
        @Schema(description = "해당 슬롯에 지원한 지원자 목록")
        private final List<ApplicantInfo> applicants;

        public static SlotApplicantInfo of(LocalTime startTime, LocalTime endTime, List<ApplicantInfo> applicants) {
            return new SlotApplicantInfo(startTime, endTime, applicants);
        }
    }

    /**
     * 지원자 정보 DTO
     * 
     * 이름(학교/학과/포지션) 형식으로 표시됩니다.
     * 포지션이 없으면 null로 전달됩니다.
     */
    @Schema(description = "지원자 요약 정보")
    @Getter
    @RequiredArgsConstructor
    public static class ApplicantInfo {

        @Schema(description = "지원자 ID", example = "1")
        private final Long applicantId;
        @Schema(description = "이름", example = "홍길동")
        private final String name;
        @Schema(description = "학교", example = "캠퍼스대학교")
        private final String school;
        @Schema(description = "전공", example = "컴퓨터공학과")
        private final String major;
        @Schema(description = "지원 포지션 (없을 수 있음)", example = "백엔드")
        private final String position; // null 가능

        public static ApplicantInfo of(Long applicantId, String name, String school, String major, String position) {
            return new ApplicantInfo(applicantId, name, school, major, position);
        }
    }
}
