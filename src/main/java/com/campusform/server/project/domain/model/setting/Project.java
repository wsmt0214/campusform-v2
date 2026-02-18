package com.campusform.server.project.domain.model.setting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.campusform.server.project.domain.exception.ProjectAccessDeniedException;
import com.campusform.server.project.domain.model.setting.value.ProjectState;
import com.campusform.server.project.domain.model.setting.value.RequiredFieldMapping;
import com.campusform.server.project.domain.model.setting.value.SyncStatus;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트(모집 공고) Entity
 * 
 * 채용 프로세스의 단위로, 모집 기간, 관리자, 스프레드시트 연동 정보를 관리합니다.
 * 애그리거트 루트 역할을 하며, ProjectAdmin과 ProjectRequiredMapping을 포함합니다.
 */
@Entity
@Table(name = "projects", indexes = @Index(name = "idx_owner_id", columnList = "owner_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Project {

    /**
     * 프로젝트 제목 정책
     * - 최대 40자
     * - 한글/영문/특수문자/공백만 허용 (숫자/기타 문자 집합은 허용하지 않음)
     */
    private static final int MAX_TITLE_LENGTH = 40;
    private static final Pattern TITLE_ALLOWED_PATTERN = Pattern.compile("^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9\\p{Punct}\\s]+$");

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectState state = ProjectState.DOCUMENT;

    @Column(name = "sheet_url", nullable = false)
    private String sheetUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_status", nullable = false)
    private SyncStatus lastSyncStatus = SyncStatus.OK;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "start_at", nullable = false)
    private LocalDate startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDate endAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectAdmin> admins = new ArrayList<>();

    @OneToOne(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProjectRequiredMapping mapping = new ProjectRequiredMapping();

    /** 값 치환 규칙. 동기화 시 적용됨 */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectValueMapping> valueMappings = new ArrayList<>();

    /**
     * 사용자가 프로젝트의 Owner인지 검증
     */
    public void validateOwnerAccess(Long userId) {
        if (!this.ownerId.equals(userId)) {
            throw new ProjectAccessDeniedException("프로젝트 OWNER만 접근할 수 있습니다.");
        }
    }

    /**
     * 사용자가 프로젝트의 관리자(Owner 또는 Admin)인지 검증
     */
    public void validateAdminAccess(Long userId) {
        if (this.ownerId.equals(userId)) {
            return; // Owner는 항상 관리자
        }

        boolean isAdmin = this.admins.stream()
                .anyMatch(admin -> admin.getAdminId().equals(userId));

        if (!isAdmin) {
            throw new IllegalArgumentException(
                    "해당 사용자는 프로젝트의 관리자가 아니거나 접근할 권한이 없습니다. userId=" + userId + ", projectId=" + this.id);
        }
    }

    /** 프프로젝트 생성 팩토리 메서드 */
    public static Project create(String title, Long ownerId, String sheetUrl, LocalDate startAt, LocalDate endAt) {
        validate(title, ownerId, sheetUrl, startAt, endAt);
        Project project = new Project();
        // 저장 시에는 trim을 적용하여 일관된 값을 저장합니다.
        project.title = title.trim();
        project.ownerId = ownerId;
        project.sheetUrl = sheetUrl;
        project.startAt = startAt;
        project.endAt = endAt;
        return project;
    }

    /** 관리자 추가 연관관계 편의메서드 */
    public void addAdmin(Long adminId) {
        if (adminId == null)
            throw new IllegalArgumentException("adminId가 필요합니다.");
        if (hasAdmin(adminId))
            throw new IllegalArgumentException("이미 추가된 관리자입니다.");

        admins.add(ProjectAdmin.create(this, adminId));
    }

    /**
     * 관리자 제거 연관관계 편의메서드
     * 
     * @param adminId 제거할 관리자 ID
     * @throws IllegalArgumentException 관리자가 존재하지 않는 경우
     */
    public void removeAdmin(Long adminId) {
        if (adminId == null) {
            throw new IllegalArgumentException("adminId가 필요합니다.");
        }

        // OWNER는 제거할 수 없음
        if (this.ownerId.equals(adminId)) {
            throw new IllegalArgumentException("프로젝트 OWNER는 관리자에서 제거할 수 없습니다.");
        }

        // 관리자 목록에서 제거
        boolean removed = admins.removeIf(admin -> admin.getAdminId().equals(adminId));

        if (!removed) {
            throw new IllegalArgumentException("해당 사용자는 프로젝트 관리자가 아닙니다. adminId=" + adminId);
        }
    }

    /** 필수 필드 매핑 정보 설정 연관관계 편의메서드 */
    public void addMapping(RequiredFieldMapping mappingValue) {
        this.mapping = ProjectRequiredMapping.create(this, mappingValue);
    }

    /**
     * 포지션 값 치환 규칙 추가
     */
    public void addValueMapping(String fromValue, String toValue) {
        valueMappings.add(ProjectValueMapping.create(this, fromValue, toValue));
    }

    // ── 상태 검증 도메인 메서드 ──

    /** 프로젝트가 종료 상태인지 확인 */
    public boolean isCompleted() {
        return state == ProjectState.DOCUMENT_COMPLETE || state == ProjectState.INTERVIEW_COMPLETE;
    }

    /** 프로젝트가 종료됐으면 예외 발생 */
    public void validateNotCompleted() {
        if (isCompleted()) {
            throw new IllegalStateException("이미 종료된 프로젝트입니다. 현재 상태: " + state);
        }
    }

    /** 서류 단계(DOCUMENT)에서만 가능한 작업에 대한 검증 */
    public void validateDocumentStage() {
        if (state != ProjectState.DOCUMENT) {
            throw new IllegalStateException("서류 단계(DOCUMENT)에서만 가능합니다. 현재 상태: " + state);
        }
    }

    /** 면접 단계(INTERVIEW)에서만 가능한 작업에 대한 검증 */
    public void validateInterviewStage() {
        if (state != ProjectState.INTERVIEW) {
            throw new IllegalStateException("면접 단계(INTERVIEW)에서만 가능합니다. 현재 상태: " + state);
        }
    }

    // ── 상태 전환 메서드 ──

    /**
     * 면접 단계로 전환: DOCUMENT → INTERVIEW
     * (별도 API PATCH /api/recruiting/projects/{projectId}/start-interview 로 호출)
     */
    public void startInterview() {
        if (state != ProjectState.DOCUMENT) {
            throw new IllegalStateException("면접 단계 전환은 서류 단계(DOCUMENT)에서만 가능합니다. 현재 상태: " + state);
        }
        this.state = ProjectState.INTERVIEW;
    }

    /**
     * 서류 단계 종료 (면접 없이 프로젝트 종료): DOCUMENT → DOCUMENT_COMPLETE
     *
     * OWNER만 가능합니다.
     */
    public void completeDocument(Long userId) {
        validateOwnerAccess(userId);
        if (state != ProjectState.DOCUMENT) {
            throw new IllegalStateException("서류 단계 종료는 DOCUMENT 상태에서만 가능합니다. 현재 상태: " + state);
        }
        this.state = ProjectState.DOCUMENT_COMPLETE;
    }

    /**
     * 면접 단계 종료 (프로젝트 전체 종료): INTERVIEW → INTERVIEW_COMPLETE
     *
     * OWNER만 가능합니다.
     */
    public void completeAll(Long userId) {
        validateOwnerAccess(userId);
        if (state != ProjectState.INTERVIEW) {
            throw new IllegalStateException("면접 종료는 INTERVIEW 상태에서만 가능합니다. 현재 상태: " + state);
        }
        this.state = ProjectState.INTERVIEW_COMPLETE;
    }

    /**
     * 테스트용: 프로젝트 상태를 직접 설정
     * 
     * 주의: 이 메서드는 테스트 목적으로만 사용해야 합니다.
     * 프로덕션 환경에서는 비즈니스 로직에 맞는 상태 전환 메서드를 사용해야 합니다.
     * 
     * @param state 설정할 프로젝트 상태
     */
    public void setStateForTest(ProjectState state) {
        if (state == null) {
            throw new IllegalArgumentException("상태는 null일 수 없습니다.");
        }
        this.state = state;
    }

    /**
     * 시트 동기화 상태 업데이트
     * 
     * @param status 동기화 상태 (OK 또는 ERROR)
     */
    public void updateSyncStatus(SyncStatus status) {
        this.lastSyncStatus = status;
        this.lastSyncedAt = LocalDateTime.now();
    }

    /**
     * 프로젝트의 모든 관리자 ID 목록 조회 (OWNER 포함, 중복 제거)
     */
    public List<Long> getAdminIds() {
        Set<Long> adminIds = new LinkedHashSet<>();
        adminIds.add(this.ownerId);
        for (ProjectAdmin admin : this.admins) {
            adminIds.add(admin.getAdminId());
        }
        return List.copyOf(adminIds);
    }

    /** 관리자 중복 여부 확인 */
    private boolean hasAdmin(Long adminId) {
        return admins.stream().anyMatch(admin -> adminId.equals(admin.getAdminId()));
    }

    /**
     * 프로젝트 제목(이름) 수정
     *
     * @param newTitle 새로운 제목 (공백만으로 구성될 수 없음)
     */
    public void updateTitle(String newTitle) {
        validateTitle(newTitle);
        this.title = newTitle.trim();
    }

    /**
     * 프로젝트 모집 기간(시작일·종료일) 수정
     *
     * @param startAt 모집 시작일
     * @param endAt   모집 종료일 (시작일 이후여야 함)
     */
    public void updatePeriod(LocalDate startAt, LocalDate endAt) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("모집 시작일과 종료일은 필수입니다.");
        }
        if (endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("모집 종료일은 시작일 이후여야 합니다.");
        }
        this.startAt = startAt;
        this.endAt = endAt;
    }

    /** 프로젝트 생성 시 유효성 검사 */
    private static void validate(String title, Long ownerId, String sheetUrl, LocalDate startAt, LocalDate endAt) {
        validateTitle(title);
        if (sheetUrl.isBlank())
            throw new IllegalArgumentException("sheetUrl가 필요합니다.");
        if (endAt.isBefore(startAt))
            throw new IllegalArgumentException("endAt은 startAt 이후여야 합니다.");
    }

    /**
     * 프로젝트 제목 유효성 검증
     * 생성/수정에서 동일한 정책을 적용합니다.
     */
    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("프로젝트 이름은 필수이며, 공백만으로 구성될 수 없습니다.");
        }

        String trimmed = title.trim();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("프로젝트 이름은 최대 " + MAX_TITLE_LENGTH + "자까지 가능합니다.");
        }

        // 한글/영문/숫자/특수문자/공백만 허용 (이모지, 기타 문자는 불가)
        if (!TITLE_ALLOWED_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("프로젝트 이름은 한글, 영문, 숫자, 특수문자, 공백만 입력할 수 있습니다.");
        }
    }
}
