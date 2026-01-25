package com.campusform.server.recruiting.domain.model.applicant;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원자 추가 질문 답변 Entity
 * 시트에서 고정 컬럼으로 매핑되지 않은 질문을 별도 테이블로 관리합니다.
 */
@Entity
@Table(name = "applicant_extra_answers")
// indexes = @Index(name = "idx_applicant_id", columnList = "applicant_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ApplicantExtraAnswer {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ApplicantExtraAnswer create(Applicant applicant, String questionText, String answerText) {
        ApplicantExtraAnswer extraAnswer = new ApplicantExtraAnswer();
        extraAnswer.applicant = applicant;
        extraAnswer.questionText = questionText;
        extraAnswer.answerText = answerText;
        return extraAnswer;
    }
}
