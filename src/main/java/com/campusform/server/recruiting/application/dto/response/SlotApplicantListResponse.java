package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 모든 슬롯별 지원자 목록 조회 응답 DTO
 * 
 * 날짜별 -> 슬롯별 -> 지원자 목록 구조로 제공됩니다.
 */
@Getter
@RequiredArgsConstructor
public class SlotApplicantListResponse {

    private final List<DaySlotSummary> summaries;

    public static SlotApplicantListResponse of(List<DaySlotSummary> summaries) {
        return new SlotApplicantListResponse(summaries);
    }

    /**
     * 날짜별 슬롯 요약
     */
    @Getter
    @RequiredArgsConstructor
    public static class DaySlotSummary {

        private final LocalDate date;
        private final List<SlotApplicantInfo> slots;

        public static DaySlotSummary of(LocalDate date, List<SlotApplicantInfo> slots) {
            return new DaySlotSummary(date, slots);
        }
    }

    /**
     * 슬롯별 지원자 정보
     */
    @Getter
    @RequiredArgsConstructor
    public static class SlotApplicantInfo {

        private final LocalTime startTime;
        private final LocalTime endTime;
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
    @Getter
    @RequiredArgsConstructor
    public static class ApplicantInfo {

        private final Long applicantId;
        private final String name;
        private final String school;
        private final String major;
        private final String position; // null 가능

        public static ApplicantInfo of(Long applicantId, String name, String school, String major, String position) {
            return new ApplicantInfo(applicantId, name, school, major, position);
        }
    }
}
