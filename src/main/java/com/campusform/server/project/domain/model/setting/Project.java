package com.campusform.server.project.domain.model.setting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectState state = ProjectState.DOCUMENT_OPEN;

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

    /** 프프로젝트 생성 팩토리 메서드 */
    public static Project create(String title, Long ownerId, String sheetUrl, LocalDate startAt, LocalDate endAt) {
        validate(title, ownerId, sheetUrl, startAt, endAt);
        Project project = new Project();
        project.title = title;
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

    /** 필수 필드 매핑 정보 설정 연관관계 편의메서드 */
    public void addMapping(RequiredFieldMapping mappingValue) {
        this.mapping = ProjectRequiredMapping.create(this, mappingValue);
    }

    /**
     * 서류 단계 종료 및 프로젝트 종료
     * 
     * 전제:
     * - DOCUMENT_LOCKED 상태에서만 호출 가능합니다.
     * - 요청한 사용자가 프로젝트의 OWNER여야 합니다.
     */
    public void completeDocument(Long userId) {
        // 상태 검증: DOCUMENT_LOCKED 상태에서만 가능
        if (state != ProjectState.DOCUMENT_LOCKED) {
            throw new IllegalStateException("서류 단계 종료는 DOCUMENT_LOCKED 상태에서만 가능합니다.");
        }

        // OWNER 검증: 프로젝트의 ownerId와 요청한 사용자 ID가 일치해야 함
        if (!this.ownerId.equals(userId)) {
            throw new IllegalArgumentException("프로젝트 OWNER만 단계를 종료할 수 있습니다.");
        }

        this.state = ProjectState.DOCUMENT_DONE;
    }

    /**
     * 면접 단계 종료 및 프로젝트 종료
     * 
     * 전제:
     * - INTERVIEW_LOCKED 상태에서만 호출 가능합니다.
     * - 요청한 사용자가 프로젝트의 OWNER여야 합니다.
     */
    public void completeAll(Long userId) {
        // 상태 검증: INTERVIEW_LOCKED 상태에서만 가능
        if (state != ProjectState.INTERVIEW_LOCKED) {
            throw new IllegalStateException("전체 종료는 INTERVIEW_LOCKED 상태에서만 가능합니다.");
        }

        // OWNER 검증: 프로젝트의 ownerId와 요청한 사용자 ID가 일치해야 함
        if (!this.ownerId.equals(userId)) {
            throw new IllegalArgumentException("프로젝트 OWNER만 단계를 종료할 수 있습니다.");
        }

        this.state = ProjectState.ALL_COMPLETE;
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

    /** 관리자 중복 여부 확인인 */
    private boolean hasAdmin(Long adminId) {
        return admins.stream().anyMatch(admin -> adminId.equals(admin.getAdminId()));
    }

    /** 프로젝트 생성 시 유효성 검사 */
    private static void validate(String title, Long ownerId, String sheetUrl, LocalDate startAt, LocalDate endAt) {
        if (title.isBlank())
            throw new IllegalArgumentException("프로젝트명이 필요합니다.");
        if (sheetUrl.isBlank())
            throw new IllegalArgumentException("sheetUrl가 필요합니다.");
        if (endAt.isBefore(startAt))
            throw new IllegalArgumentException("endAt은 startAt 이후여야 합니다.");
    }
}
