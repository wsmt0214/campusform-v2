package com.campusform.server.recruiting.domain.model.applicant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 지원자 Entity
 * 지원자 필수 매핑 항목과 단계별 심사 상태를 관리합니다.
 */
@Entity
@Table(name = "applicants",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_project_name_email", columnNames = {"project_id", "name", "email"})
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Applicant {

    @Id
    @GeneratedValue
    private Long id;

    // 다른 어그리거트 -> 참조 아닌 연관으로 관계 설정
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false) private String name;
    private String school;
    private String major;
    private String gender;
    private String phone;
    @Column(nullable = false) private String email;
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

    /**
     * 지원자의 추가 질문 답변 목록
     * 
     * 방향 매핑을 통해 연관된 답변 데이터를 도메인 객체 그래프 탐색으로 쉽게 사용할 수 있도록 설계한 것입니다.
     */
    @OneToMany(mappedBy = "applicant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicantExtraAnswer> extraAnswers = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
