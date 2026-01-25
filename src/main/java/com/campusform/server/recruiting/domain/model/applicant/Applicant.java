package com.campusform.server.recruiting.domain.model.applicant;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원자 Entity
 * 지원자 필수 매핑 항목과 단계별 심사 상태를 관리합니다.
 */
@Entity
@Table(name = "applicants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_name_email", columnNames = { "project_id", "name", "email" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Applicant {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String name;
    private String school;
    private String major;
    private String gender;
    private String phone;
    @Column(nullable = false)
    private String email;
    private String position;

    /**
     * 서류 단계 심사 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false)
    private ApplicantStatus documentStatus = ApplicantStatus.HOLD;

    /**
     * 면접 단계 심사 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "interview_status")
    private ApplicantStatus interviewStatus = ApplicantStatus.HOLD;

    @Column(nullable = false)
    private Boolean bookmarked = false;

    @OneToMany(mappedBy = "applicant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicantExtraAnswer> extraAnswers = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Applicant create(Long projectId, String name, String email, String phone, String gender,
            String school, String major, String position) {
        Applicant applicant = new Applicant();
        applicant.projectId = projectId;
        applicant.name = name;
        applicant.email = email;
        applicant.phone = phone;
        applicant.gender = gender;
        applicant.school = school;
        applicant.major = major;
        applicant.position = position;
        return applicant;
    }

    public void addExtraAnswer(String questionText, String answerText) {
        extraAnswers.add(ApplicantExtraAnswer.create(this, questionText, answerText));
    }
}
