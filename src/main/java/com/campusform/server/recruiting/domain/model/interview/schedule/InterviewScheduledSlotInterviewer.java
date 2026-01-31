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
 * 슬롯에 배정된 면접관 Entity
 */
@Entity
@Table(name = "interview_scheduled_slot_interviewers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_slot_interviewer", columnNames = { "schedule_slot_id", "admin_id" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewScheduledSlotInterviewer {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_slot_id", nullable = false)
    private InterviewScheduledSlot scheduleSlot;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    public static InterviewScheduledSlotInterviewer create(InterviewScheduledSlot slot, Long adminId) {
        InterviewScheduledSlotInterviewer entity = new InterviewScheduledSlotInterviewer();
        entity.scheduleSlot = slot;
        entity.adminId = adminId;
        return entity;
    }
}
