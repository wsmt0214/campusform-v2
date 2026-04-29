package com.campusform.server.recruiting.domain.model.applicant;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.campusform.server.recruiting.domain.exception.StatusChangeNotAllowedException;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.model.event.ApplicantUpdated;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원자 Entity
 */
@Entity
@Table(name = "applicants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_name_email", columnNames = { "project_id", "name", "email" })
}, indexes = {
        @Index(name = "idx_project_document_interview", columnList = "project_id, document_status, interview_status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Applicant extends AbstractAggregateRoot<Applicant> {

    @Id
    @GeneratedValue
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String name;
    private String school;
    private String major;
    private String gender;
    private String phone;
    private String email;
    private String position;

    /**
     * 서류 단계 심사 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false)
    private ScreeningResult documentStatus = ScreeningResult.HOLD;
    /**
     * 면접 단계 심사 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "interview_status")
    private ScreeningResult interviewStatus = ScreeningResult.HOLD;

    /**
     * 서류 단계 즐겨찾기 여부
     */
    @Column(name = "document_bookmarked", nullable = false)
    private Boolean documentBookmarked = false;

    /**
     * 면접 단계 즐겨찾기 여부
     */
    @Column(name = "interview_bookmarked", nullable = false)
    private Boolean interviewBookmarked = false;

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

    public void addExtraAnswer(String questionText, String answerText, Integer orderIndex) {
        extraAnswers.add(ApplicantExtraAnswer.create(this, questionText, answerText, orderIndex));
    }

    public void updateBasicFieldsFromSheet(String phone, String gender, String school, String major, String position) {
        this.phone = phone;
        this.gender = gender;
        this.school = school;
        this.major = major;
        this.position = position;
    }

    public void syncExtraAnswersFromSheet(List<SheetExtraAnswer> newAnswers) {
        Map<Integer, ApplicantExtraAnswer> existingByOrderIndex = new HashMap<>();
        for (ApplicantExtraAnswer a : this.extraAnswers) {
            if (a.getOrderIndex() == null) {
                continue;
            }
            existingByOrderIndex.put(a.getOrderIndex(), a);
        }

        Map<Integer, SheetExtraAnswer> incomingByOrderIndex = new HashMap<>();
        for (SheetExtraAnswer incoming : newAnswers) {
            if (incoming.orderIndex() == null) {
                continue;
            }
            incomingByOrderIndex.put(incoming.orderIndex(), incoming);
        }

        // update or insert
        for (SheetExtraAnswer incoming : incomingByOrderIndex.values()) {
            ApplicantExtraAnswer existing = existingByOrderIndex.remove(incoming.orderIndex());
            if (existing == null) {
                addExtraAnswer(incoming.questionText(), incoming.answerText(), incoming.orderIndex());
                continue;
            }
            String normalizedNew = normalizeAnswer(incoming.answerText());
            String normalizedOld = normalizeAnswer(existing.getAnswerText());
            if (!Objects.equals(normalizedNew, normalizedOld) || !Objects.equals(incoming.questionText(), existing.getQuestionText())) {
                existing.updateAnswer(incoming.questionText(), incoming.answerText());
            }
        }

        // delete remaining (not present anymore)
        if (!existingByOrderIndex.isEmpty()) {
            this.extraAnswers.removeIf(a -> a.getOrderIndex() != null && existingByOrderIndex.containsKey(a.getOrderIndex()));
        }
    }

    private static String normalizeAnswer(String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.trim();
        return v.isEmpty() ? "" : v;
    }

    public record SheetExtraAnswer(
            String questionText,
            String answerText,
            Integer orderIndex
    ) {
    }

    /**
     * 면접 단계 진입 가능 여부 검증용 메서드
     * 
     * 서류 합격자가 아닌 경우 면접 단계 작업을 수행할 수 없음
     */
    public void validateInterviewEligibility(RecruitmentStage stage) {
        if (stage == RecruitmentStage.INTERVIEW && this.documentStatus != ScreeningResult.PASS) {
            throw new StatusChangeNotAllowedException(
                    "면접 단계로 진행하려면 서류 단계에서 합격(PASS) 상태여야 합니다. 현재 서류 상태: "
                            + this.documentStatus);
        }
    }

    /**
     * [비즈니스 로직] 서류 심사 결과 업데이트 및 이벤트 발행
     * 
     * 면접 단계로 진행하려면 서류 단계에서 합격(PASS) 상태여야 함
     */
    public void updateScreeningResult(RecruitmentStage stage, ScreeningResult status) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        if (stage == RecruitmentStage.DOCUMENT) {
            if (this.documentStatus == status) {
                return;
            }
            this.documentStatus = status;
        } else if (stage == RecruitmentStage.INTERVIEW) {
            validateInterviewEligibility(stage);
            if (this.interviewStatus == status) {
                return;
            }
            this.interviewStatus = status;
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
     * 포지션 값 치환 규칙 변경 시 지원자 position만 갱신 (다른 필드·추가답변 유지)
     */
    public void updatePosition(String position) {
        this.position = position != null ? position.trim() : null;
    }

    /**
     * 지원자 정보를 업데이트합니다.
     * 
     * 시트 동기화 시 기존 지원자의 정보를 최신 데이터로 갱신합니다.
     * 심사 상태와 즐겨찾기는 유지하고, 기본 정보와 추가 답변만 업데이트합니다.
     */
    public void updateFromSheet(String phone, String gender, String school, String major, String position) {
        updateBasicFieldsFromSheet(phone, gender, school, major, position);
        // 기존 구현 호환을 위해 유지, Step 6에서는 updateBasicFieldsFromSheet + syncExtraAnswersFromSheet 사용
        this.extraAnswers.clear();
    }

    /**
     * 성별이 남성인지 여부
     */
    public boolean isMale() {
        if (gender == null) return false;
        String g = gender.trim();
        return "남".equals(g) || "남자".equals(g) || "남성".equals(g) || "male".equalsIgnoreCase(g);
    }

    /**
     * 성별이 여성인지 여부
     */
    public boolean isFemale() {
        if (gender == null) return false;
        String g = gender.trim();
        return "여".equals(g) || "여자".equals(g) || "여성".equals(g) || "female".equalsIgnoreCase(g);
    }

    /**
     * 단계별 즐겨찾기 여부 조회
     */
    public boolean isBookmarkedFor(RecruitmentStage stage) {
        if (stage == RecruitmentStage.DOCUMENT) {
            return Boolean.TRUE.equals(documentBookmarked);
        }
        if (stage == RecruitmentStage.INTERVIEW) {
            return Boolean.TRUE.equals(interviewBookmarked);
        }
        return false;
    }

    /**
     * 단계별 즐겨찾기 토글
     */
    public void toggleBookmark(RecruitmentStage stage) {
        if (stage == RecruitmentStage.DOCUMENT) {
            this.documentBookmarked = !Boolean.TRUE.equals(this.documentBookmarked);
        } else if (stage == RecruitmentStage.INTERVIEW) {
            this.interviewBookmarked = !Boolean.TRUE.equals(this.interviewBookmarked);
        }
    }
}
