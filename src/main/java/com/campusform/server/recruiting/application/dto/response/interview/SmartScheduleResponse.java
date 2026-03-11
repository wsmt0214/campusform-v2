package com.campusform.server.recruiting.application.dto.response.interview;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.campusform.server.recruiting.domain.model.interview.schedule.value.UnassignmentReason;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스마트 시간표 알고리즘 응답 DTO
 * 
 * 날짜별로 그룹핑되어 슬롯 정보와 지원자/면접관 상세 정보를 포함합니다.
 */
@Schema(description = "스마트 시간표 생성 결과 응답")
@Getter
@RequiredArgsConstructor
public class SmartScheduleResponse {

    @Schema(description = "날짜별 배정 결과")
    private final List<DaySummary> days;

    @Schema(description = "미배정된 지원자 목록")
    private final List<UnassignedApplicantInfo> unassignedApplicants;

    @Schema(description = "알고리즘 실행 통계")
    private final Statistics statistics;

    @Schema(description = "일자별 배정 요약")
    @Getter
    @RequiredArgsConstructor
    public static class DaySummary {
        @Schema(description = "날짜", example = "2024-07-01")
        private final LocalDate date;
        @Schema(description = "해당 날짜의 배정된 슬롯 목록")
        private final List<SlotInfo> slots;

        public static DaySummary of(LocalDate date, List<SlotInfo> slots) {
            return new DaySummary(date, slots);
        }
    }

    @Schema(description = "슬롯 배정 정보")
    @Getter
    @RequiredArgsConstructor
    public static class SlotInfo {
        @Schema(description = "슬롯 시작 시간", example = "10:00")
        private final LocalTime startTime;
        @Schema(description = "슬롯 종료 시간", example = "10:20")
        private final LocalTime endTime;
        @Schema(description = "해당 슬롯에 배정된 지원자 목록")
        private final List<ApplicantInfo> applicants;
        @Schema(description = "해당 슬롯에 배정된 면접관 목록")
        private final List<InterviewerInfo> interviewers;

        public static SlotInfo of(LocalTime startTime, LocalTime endTime,
                List<ApplicantInfo> applicants, List<InterviewerInfo> interviewers) {
            return new SlotInfo(startTime, endTime, applicants, interviewers);
        }
    }

    @Schema(description = "지원자 정보")
    @Getter
    @RequiredArgsConstructor
    public static class ApplicantInfo {
        @Schema(description = "지원자 ID", example = "1")
        private final Long id;
        @Schema(description = "지원자 이름", example = "홍길동")
        private final String name;
        @Schema(description = "지원자 학교", example = "캠퍼스대학교")
        private final String school;
        @Schema(description = "지원자 전공", example = "컴퓨터공학과")
        private final String major;
        @Schema(description = "지원 포지션", example = "백엔드")
        private final String position;

        public static ApplicantInfo of(Long id, String name, String school, String major, String position) {
            return new ApplicantInfo(id, name, school, major, position);
        }
    }

    @Schema(description = "면접관 정보")
    @Getter
    @RequiredArgsConstructor
    public static class InterviewerInfo {
        @Schema(description = "면접관 사용자 ID", example = "101")
        private final Long id;
        @Schema(description = "면접관 이름", example = "김면접")
        private final String name;
        @Schema(description = "필수 면접관 여부", example = "true")
        private final boolean required;

        public static InterviewerInfo of(Long id, String name, boolean required) {
            return new InterviewerInfo(id, name, required);
        }
    }

    @Schema(description = "미배정 지원자 정보")
    @Getter
    @RequiredArgsConstructor
    public static class UnassignedApplicantInfo {
        @Schema(description = "지원자 ID", example = "2")
        private final Long id;
        @Schema(description = "지원자 이름", example = "이영희")
        private final String name;
        @Schema(description = "지원자 학교", example = "캠퍼스대학교")
        private final String school;
        @Schema(description = "지원자 전공", example = "소프트웨어학과")
        private final String major;
        @Schema(description = "지원 포지션", example = "프론트엔드")
        private final String position;
        @Schema(description = "미배정 사유", example = "모든 가능 시간에 면접관 부족")
        private final String reason;

        public static UnassignedApplicantInfo of(ApplicantInfo applicant, UnassignmentReason reason) {
            return new UnassignedApplicantInfo(
                    applicant.getId(), applicant.getName(), applicant.getSchool(),
                    applicant.getMajor(), applicant.getPosition(), reason.getMessage());
        }
    }

    @Schema(description = "시간표 생성 통계")
    @Getter
    @RequiredArgsConstructor
    public static class Statistics {
        @Schema(description = "총 지원자 수", example = "100")
        private final int totalApplicants;
        @Schema(description = "배정된 지원자 수", example = "95")
        private final int assignedApplicants;
        @Schema(description = "미배정된 지원자 수", example = "5")
        private final int unassignedApplicants;
        @Schema(description = "사용된 총 슬롯 수", example = "32")
        private final int usedSlots;
        @Schema(description = "배정률 (%)", example = "95.0")
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
