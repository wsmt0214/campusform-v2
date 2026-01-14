package com.campusform.server.project.domain.model.setting;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.campusform.server.project.domain.model.setting.value.ProjectRole;

import java.time.LocalDateTime;

/**
 * 프로젝트 관리자 Entity
 * 프로젝트별 ADMIN 관리를 담당합니다.
 */
@Entity
@Table(name = "project_admins",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_project_admin", columnNames = {"project_id", "admin_id"})
       },
       indexes = @Index(name = "idx_project_id", columnList = "project_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProjectAdmin {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 프로젝트 (부모 Aggregate Root)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * 관리자 ID (Identity Context 참조이므로 ID만 저장)
     */
    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    /**
     * 관리자 역할 (확장 대비)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role = ProjectRole.ADMIN;

    /**
     * 관리자 추가 시간
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
