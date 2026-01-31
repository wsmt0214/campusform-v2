package com.campusform.server.recruiting.domain.model.interview.schedule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 슬롯에 배정된 지원자 Entity
 * 배정된 슬롯과 지원자의 다대다 관계를 관리합니다.
 */
@Entity
@Table(name = "interview_scheduled_slot_applicants", uniqueConstraints = {
        // 같은 슬롯에 같은 지원자가 중복으로 들어가는 것을 방지
        @UniqueConstraint(name = "uk_slot_applicant", columnNames = { "schedule_slot_id", "applicant_id" }),
        // 한 지원자는 전체 스케줄에서 단 1개의 슬롯에만 배정될 수 있음(중복 배정 방지)
        @UniqueConstraint(name = "uk_applicant_single_slot", columnNames = { "applicant_id" })
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

    public static InterviewScheduledSlotApplicant create(InterviewScheduledSlot slot, Long applicantId) {
        InterviewScheduledSlotApplicant entity = new InterviewScheduledSlotApplicant();
        entity.scheduleSlot = slot;
        entity.applicantId = applicantId;
        return entity;
    }
}
