package com.campusform.server.project.domain.model.setting;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.campusform.server.project.domain.model.setting.value.ProjectState;
import com.campusform.server.project.domain.model.setting.value.SyncStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 프로젝트(모집 공고) Entity
 * Project Context의 핵심 도메인 모델입니다.
 */
@Entity
@Table(name = "projects",
       indexes = @Index(name = "idx_owner_id", columnList = "owner_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Project {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String title;

    // 다른 어그리거트 -> 참조 아닌 연관으로 관계 설정
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

    /**
     * 모집 시작일
     */
    @Column(name = "start_at", nullable = false)
    private LocalDate startAt;

    /**
     * 모집 종료일
     */
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
}
