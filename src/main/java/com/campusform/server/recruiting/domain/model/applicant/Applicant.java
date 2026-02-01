package com.campusform.server.recruiting.domain.model.applicant;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import com.campusform.server.recruiting.domain.model.applicant.value.StageStatus;
import com.campusform.server.recruiting.domain.model.event.ApplicantUpdated;

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
public class Applicant extends AbstractAggregateRoot<Applicant> {

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
    private StageStatus stage;
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

    // 누르면 true <-> false 바뀜
    public void Bookmark() {
        this.bookmarked = !this.bookmarked;
    }

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

    /**
     * [비즈니스 로직] 서류 심사 결과 업데이트 및 이벤트 발행
     */
    public void updateApplicantStatus(StageStatus stage, ApplicantStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("newStatus must not be null");
        }
        if (stage == StageStatus.DOCUMENT) {
            if (this.documentStatus == status) {
                return;
            }
            this.documentStatus = status;
        } else {
            if (stage == StageStatus.INTERVIEW) {
                if (this.interviewStatus == status) {
                    return;
                }
                this.interviewStatus = status;
            }
        }

        this.registerEvent(new ApplicantUpdated(
                this.id,
                this.projectId,
                this.name,
                this.phone,
                this.position,
                status,
                stage));
    }

    /**
     * 지원자 정보를 업데이트합니다.
     * 
     * 시트 동기화 시 기존 지원자의 정보를 최신 데이터로 갱신합니다.
     * 심사 상태와 즐겨찾기는 유지하고, 기본 정보와 추가 답변만 업데이트합니다.
     */
    public void updateFromSheet(String phone, String gender, String school, String major, String position) {
        this.phone = phone;
        this.gender = gender;
        this.school = school;
        this.major = major;
        this.position = position;
        // extraAnswers는 orphanRemoval=true이므로 리스트를 비우면 자동 삭제됨
        this.extraAnswers.clear();
    }
}
