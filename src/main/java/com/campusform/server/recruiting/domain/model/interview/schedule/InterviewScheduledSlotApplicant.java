package com.campusform.server.recruiting.domain.model.interview.schedule;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 슬롯에 배정된 지원자 Entity
 * 배정된 슬롯과 지원자의 다대다 관계를 관리합니다.
 */
@Entity
@Table(name = "interview_scheduled_slot_applicants",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_slot_applicant", columnNames = {"schedule_slot_id", "applicant_id"})
       })
      // indexes = @Index(name = "idx_applicant_id", columnList = "applicant_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewScheduledSlotApplicant {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_slot_id", nullable = false)
    private InterviewScheduledSlot scheduleSlot;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;
}
