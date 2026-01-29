package com.campusform.server.recruiting.domain.model.interview.setup;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;  
         
/**
 * 필수 면접관 Entity
 * 필수 면접관 관리를 담당합니다.
 */
@Entity
@Table(name = "interview_required_interviewers",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_setting_admin", columnNames = {"interview_setting_id", "admin_id"})
       })
       // indexes = @Index(name = "idx_setting_id", columnList = "interview_setting_id"))
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

    @Column(nullable = false)
    private Boolean required = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
