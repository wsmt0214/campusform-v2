package com.campusform.server.project.domain.model.setting;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.campusform.server.project.domain.model.setting.value.ProjectRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 관리자 Entity
 * 프로젝트별 ADMIN 관리를 담당합니다.
 */
@Entity
@Table(name = "project_admins", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_admin", columnNames = { "project_id", "admin_id" })
}, indexes = @Index(name = "idx_project_id", columnList = "project_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProjectAdmin {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role = ProjectRole.ADMIN;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 프로젝트 관리자 생성 팩토리 메서드
     * 
     * @param project 프로젝트
     * @param adminId 관리자 ID
     * @return 생성된 ProjectAdmin 인스턴스
     */
    static ProjectAdmin create(Project project, Long adminId) {
        ProjectAdmin admin = new ProjectAdmin();
        admin.project = project;
        admin.adminId = adminId;
        return admin;
    }
}
