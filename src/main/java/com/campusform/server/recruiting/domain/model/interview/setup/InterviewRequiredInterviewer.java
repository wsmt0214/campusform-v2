package com.campusform.server.recruiting.domain.model.interview.setup;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
 * 필수 면접관 Entity
 * 필수 면접관 관리를 담당합니다.
 */
@Entity
@Table(name = "interview_required_interviewers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_setting_admin", columnNames = { "interview_setting_id", "admin_id" })
})
// indexes = @Index(name = "idx_setting_id", columnList =
// "interview_setting_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InterviewRequiredInterviewer {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_setting_id", nullable = false)
    private InterviewSetting setting;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 필수 면접관 생성 팩토리 메서드
     */
    public static InterviewRequiredInterviewer create(InterviewSetting setting, Long adminId) {
        InterviewRequiredInterviewer requiredInterviewer = new InterviewRequiredInterviewer();
        requiredInterviewer.setting = setting;
        requiredInterviewer.adminId = adminId;
        return requiredInterviewer;
    }
}
