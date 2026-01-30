package com.campusform.server.recruiting.domain.model.interview.availability;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원자 면접 가능 슬롯 Entity
 * 제출된 지원자 면접 가능 슬롯을 관리합니다.
 */
@Entity
@Table(name = "interviewee_availability_slots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IntervieweeAvailabilitySlot {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "interview_day_id", nullable = false)
    private Long interviewDayId;

    /**
     * 시작 시간 (end_time은 start_time + slot_duration_min)
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * IntervieweeAvailabilitySlot 생성 팩토리 메서드
     */
    public static IntervieweeAvailabilitySlot create(Long applicantId, Long interviewDayId, LocalTime startTime) {
        IntervieweeAvailabilitySlot slot = new IntervieweeAvailabilitySlot();
        slot.applicantId = applicantId;
        slot.interviewDayId = interviewDayId;
        slot.startTime = startTime;
        return slot;
    }
}
