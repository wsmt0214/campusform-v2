package com.campusform.server.recruiting.domain.model.interview.schedule;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자동 배정 실패한 지원자 Entity
 * 스마트 시간표 알고리즘에서 배정 실패한 지원자와 사유를 관리합니다.
 */
@Entity
@Table(name = "interview_schedule_unassigned_applicants",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_project_applicant", columnNames = {"project_id", "applicant_id"})
       })
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
}
