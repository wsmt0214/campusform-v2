package com.campusform.server.recruiting.domain.model.interview.schedule;

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
 * 미배정 지원자 애그리거트 루트
 * 
 * 스마트 시간표 알고리즘에서 배정 실패한 지원자와 사유를 관리합니다.
 */
@Entity
@Table(name = "interview_schedule_unassigned_applicants", uniqueConstraints = @UniqueConstraint(name = "uk_project_applicant", columnNames = {
        "project_id", "applicant_id" }), indexes = @Index(name = "idx_project_id", columnList = "project_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewScheduleUnassignedApplicant {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    // ========== 팩토리 메서드 ==========

    public static InterviewScheduleUnassignedApplicant create(
            Long projectId,
            Long applicantId,
            String reason) {
        InterviewScheduleUnassignedApplicant entity = new InterviewScheduleUnassignedApplicant();
        entity.projectId = projectId;
        entity.applicantId = applicantId;
        entity.reason = reason;
        return entity;
    }
}
