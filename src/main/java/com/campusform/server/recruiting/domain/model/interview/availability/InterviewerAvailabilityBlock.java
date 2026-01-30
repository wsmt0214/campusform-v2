package com.campusform.server.recruiting.domain.model.interview.availability;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 면접관 가능 시간 블록 Entity
 * 면접관 30분 단위 가능 시간 후보를 관리합니다.
 */
@Entity
@Table(name = "interviewer_availability_blocks",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_admin_day_time", columnNames = {"admin_id", "interview_day_id", "start_time"})
       },
       indexes = {
          // @Index(name = "idx_interview_day_time", columnList = "interview_day_id, start_time")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InterviewerAvailabilityBlock { 

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "interview_day_id", nullable = false)
    private Long interviewDayId;

    /**
     * 시작 시간 (end_time은 start_time + 30분)
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * InterviewerAvailabilityBlock 생성 팩토리 메서드
     */
    public static InterviewerAvailabilityBlock create(Long adminId, Long interviewDayId, LocalTime startTime) {
        InterviewerAvailabilityBlock block = new InterviewerAvailabilityBlock();
        block.adminId = adminId;
        block.interviewDayId = interviewDayId;
        block.startTime = startTime;
        return block;
    }
}
