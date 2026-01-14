package com.campusform.server.recruiting.domain.model.interview.setup;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 면접 설정 Entity
 * 면접 관련 규칙 및 정책을 관리합니다.
 */
@Entity
@Table(name = "interview_settings",
       indexes = @Index(name = "idx_project_id", columnList = "project_id", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InterviewSetting {

    @Id
    @GeneratedValue
    private Long id;

    // 다른 어그리거트 -> 참조 아닌 연관으로 관계 설정
    @Column(name = "project_id", nullable = false, unique = true)
    private Long projectId;

    /**
     * 면접 시작 시간
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * 면접 종료 시간
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * 슬롯 길이 (분 단위)
     * 슬롯 = duration + break
     */
    @Column(name = "slot_duration_min", nullable = false)
    private Integer slotDurationMin;

    /**
     * 슬롯 간 휴식 시간 (분 단위)
     */
    @Column(name = "slot_break_min", nullable = false)
    private Integer slotBreakMin = 0;

    /**
     * 슬롯당 최대 지원자 수
     */
    @Column(name = "max_applicants_per_slot", nullable = false)
    private Integer maxApplicantsPerSlot;

    /**
     * 슬롯당 최소 면접관 수
     */
    @Column(name = "min_interviewers_per_slot", nullable = false)
    private Integer minInterviewersPerSlot;

    /**
     * 슬롯당 최대 면접관 수
     */
    @Column(name = "max_interviewers_per_slot", nullable = false)
    private Integer maxInterviewersPerSlot;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // InterviewSetting (루트)
    @OneToMany(mappedBy = "setting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewDay> days = new ArrayList<>();

    @OneToMany(mappedBy = "setting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewRequiredInterviewer> requiredInterviewers = new ArrayList<>();

    @OneToOne(mappedBy = "setting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private InterviewAvailabilityInvestigationLink investigationLink;
    
}
