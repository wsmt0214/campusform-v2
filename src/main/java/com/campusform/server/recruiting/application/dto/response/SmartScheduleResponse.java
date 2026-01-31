package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.campusform.server.recruiting.domain.model.interview.schedule.value.UnassignmentReason;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스마트 시간표 알고리즘 응답 DTO
 * 
 * 날짜별로 그룹핑되어 슬롯 정보와 지원자/면접관 상세 정보를 포함합니다.
 */
@Getter
@RequiredArgsConstructor
public class SmartScheduleResponse {

    // 날짜별 배정 결과
    private final List<DaySummary> days;

    // 미배정 지원자 목록
    private final List<UnassignedApplicantInfo> unassignedApplicants;

    // 알고리즘 실행 통계
    private final Statistics statistics;

    @Getter
    @RequiredArgsConstructor
    public static class DaySummary {
        private final LocalDate date;
        private final List<SlotInfo> slots;

        public static DaySummary of(LocalDate date, List<SlotInfo> slots) {
            return new DaySummary(date, slots);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class SlotInfo {
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final List<ApplicantInfo> applicants;
        private final List<InterviewerInfo> interviewers;

        public static SlotInfo of(LocalTime startTime, LocalTime endTime,
                List<ApplicantInfo> applicants, List<InterviewerInfo> interviewers) {
            return new SlotInfo(startTime, endTime, applicants, interviewers);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class ApplicantInfo {
        private final Long id;
        private final String name;
        private final String school;
        private final String major;
        private final String position;

        public static ApplicantInfo of(Long id, String name, String school, String major, String position) {
            return new ApplicantInfo(id, name, school, major, position);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class InterviewerInfo {
        private final Long id;
        private final String name;
        private final boolean required;

        public static InterviewerInfo of(Long id, String name, boolean required) {
            return new InterviewerInfo(id, name, required);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class UnassignedApplicantInfo {
        private final Long id;
        private final String name;
        private final String school;
        private final String major;
        private final String position;
        private final String reason;

        public static UnassignedApplicantInfo of(ApplicantInfo applicant, UnassignmentReason reason) {
            return new UnassignedApplicantInfo(
                    applicant.getId(), applicant.getName(), applicant.getSchool(),
                    applicant.getMajor(), applicant.getPosition(), reason.getMessage());
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class Statistics {
        private final int totalApplicants;
        private final int assignedApplicants;
        private final int unassignedApplicants;
        private final int usedSlots;
        private final double assignmentRate;

        public static Statistics of(int total, int assigned, int unassigned, int usedSlots) {
            double rate = total == 0 ? 0.0 : (double) assigned / total * 100;
            return new Statistics(total, assigned, unassigned, usedSlots, rate);
        }
    }

    public static SmartScheduleResponse of(List<DaySummary> days,
            List<UnassignedApplicantInfo> unassignedApplicants,
            Statistics statistics) {
        return new SmartScheduleResponse(days, unassignedApplicants, statistics);
    }

    public static SmartScheduleResponse empty() {
        return new SmartScheduleResponse(
                List.of(),
                List.of(),
                Statistics.of(0, 0, 0, 0));
    }
}
