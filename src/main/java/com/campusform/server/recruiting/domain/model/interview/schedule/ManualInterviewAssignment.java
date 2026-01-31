package com.campusform.server.recruiting.domain.model.interview.schedule;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수동 면접 배정 Entity
 * 
 * 관리자가 수동으로 지원자에게 면접 일자(및 시간)를 배정합니다.
 * - startTime이 있으면: 특정 슬롯에 배정
 * - startTime이 null이면: 일자만 배정 (슬롯과 무관)
 * 
 * 자동 배정 알고리즘과 무관하게 독립적으로 관리됩니다.
 */
@Entity
@Table(name = "manual_interview_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_applicant", columnNames = { "applicant_id" })
}, indexes = {
        @Index(name = "idx_project_id", columnList = "project_id"),
        @Index(name = "idx_applicant_id", columnList = "applicant_id"),
        @Index(name = "idx_interview_date", columnList = "interview_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManualInterviewAssignment {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    // 수동 설정된 면접 일자
    @Column(name = "interview_date", nullable = false)
    private LocalDate interviewDate;

    // 면접 시작 시간
    @Column(name = "start_time")
    private LocalTime startTime;

    public static ManualInterviewAssignment create(
            Long projectId,
            Long applicantId,
            LocalDate interviewDate,
            LocalTime startTime) {
        ManualInterviewAssignment assignment = new ManualInterviewAssignment();
        assignment.projectId = projectId;
        assignment.applicantId = applicantId;
        assignment.interviewDate = interviewDate;
        assignment.startTime = startTime;
        return assignment;
    }

    // 시작 시간 업데이트
    public void updateStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    // 면접 일자 업데이트
    public void updateInterviewDate(LocalDate interviewDate) {
        this.interviewDate = interviewDate;
    }
}
