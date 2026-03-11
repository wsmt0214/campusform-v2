package com.campusform.server.recruiting.domain.model.interview.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import com.campusform.server.recruiting.domain.model.interview.schedule.value.UnassignmentReason;

/**
 * 스마트 시간표 알고리즘의 배정 결과를 담는 도메인 객체
 * 도메인 서비스 SmartScheduleGenerator의 반환 타입
 */
public record SchedulePlan(
        List<DayResult> days,
        List<UnassignedApplicant> unassignedApplicants,
        PlanStatistics statistics) {

    /* 날짜별 슬롯 배정 결과 */
    public record DayResult(LocalDate date, List<SlotResult> slots) {
    }

    /* 단일 슬롯 배정 결과 (지원자 + 면접관) */
    public record SlotResult(
            LocalTime startTime,
            LocalTime endTime,
            List<AssignedApplicant> applicants,
            List<AssignedInterviewer> interviewers) {
    }

    /* 슬롯에 배정된 지원자 정보 */
    public record AssignedApplicant(Long id, String name, String school, String major, String position) {
    }

    /* 슬롯에 배정된 면접관 정보 */
    public record AssignedInterviewer(Long id, String name, boolean required) {
    }

    /* 미배정 지원자 정보 — 미배정 사유는 도메인 enum으로 보존 */
    public record UnassignedApplicant(
            Long id, String name, String school, String major, String position,
            UnassignmentReason reason) {
    }

    /* 배정 결과 통계 */
    public record PlanStatistics(int totalApplicants, int assignedApplicants, int unassignedApplicants,
            int usedSlots) {
        public double assignmentRate() {
            return totalApplicants == 0 ? 0.0 : (double) assignedApplicants / totalApplicants * 100;
        }
    }

    public static SchedulePlan empty() {
        return new SchedulePlan(List.of(), List.of(), new PlanStatistics(0, 0, 0, 0));
    }
}
