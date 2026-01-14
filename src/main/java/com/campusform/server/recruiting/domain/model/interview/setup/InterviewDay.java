package com.campusform.server.recruiting.domain.model.interview.setup;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 면접 일자 Entity
 * 면접 일자 복수 관리를 담당합니다.
 */
@Entity
@Table(name = "interview_days",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_setting_interview_date", columnNames = {"interview_setting_id", "interview_date"})
       },
       indexes = @Index(name = "idx_setting_id", columnList = "interview_setting_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewDay {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 면접 설정 (부모 Aggregate Root)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_setting_id", nullable = false)
    private InterviewSetting setting;

    @Column(name = "interview_date", nullable = false)
    private LocalDate interviewDate;
}
