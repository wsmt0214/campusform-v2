package com.campusform.server.recruiting.domain.model.interview.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배정된 면접 슬롯 애그리거트 루트
 * 
 * 슬롯에 배정된 지원자와 면접관을 관리합니다.
 */
@Entity
@Table(name = "interview_scheduled_slots", uniqueConstraints = @UniqueConstraint(name = "uk_project_day_time", columnNames = {
        "project_id", "interview_day_id",
        "start_time" }), indexes = @Index(name = "idx_project_day", columnList = "project_id, interview_day_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewScheduledSlot {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "interview_day_id", nullable = false)
    private Long interviewDayId;

    // 조회 편의용
    @Column(name = "interview_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @OneToMany(mappedBy = "scheduleSlot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewScheduledSlotApplicant> applicants = new ArrayList<>();

    @OneToMany(mappedBy = "scheduleSlot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewScheduledSlotInterviewer> interviewers = new ArrayList<>();

    public static InterviewScheduledSlot create(
            Long projectId,
            Long interviewDayId,
            LocalDate date,
            LocalTime startTime) {
        InterviewScheduledSlot slot = new InterviewScheduledSlot();
        slot.projectId = projectId;
        slot.interviewDayId = interviewDayId;
        slot.date = date;
        slot.startTime = startTime;
        return slot;
    }

    // ========== 도메인 메서드 ==========

    public void addApplicant(Long applicantId) {
        this.applicants.add(InterviewScheduledSlotApplicant.create(this, applicantId));
    }

    public void addInterviewer(Long adminId) {
        this.interviewers.add(InterviewScheduledSlotInterviewer.create(this, adminId));
    }
}
