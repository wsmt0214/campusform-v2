package com.campusform.server.recruiting.domain.model.interview.schedule;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 슬롯에 배정된 면접관 Entity
 */
@Entity
@Table(name = "interview_scheduled_slot_interviewers",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_slot_interviewer", columnNames = {"schedule_slot_id", "admin_id"})
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
}
