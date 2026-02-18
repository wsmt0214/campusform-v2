package com.campusform.server.project.domain.model.setting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 포지션 값 치환 규칙 Entity
 */
@Entity
@Table(name = "project_value_mappings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_position_from", columnNames = { "project_id", "from_value" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectValueMapping {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "from_value", nullable = false)
    private String fromValue;

    @Column(name = "to_value", nullable = false)
    private String toValue;

    /**
     * 포지션 값 치환 규칙 생성 팩토리 메서드
     */
    public static ProjectValueMapping create(Project project, String fromValue, String toValue) {
        if (project == null)
            throw new IllegalArgumentException("project는 필수입니다.");
        if (fromValue == null || toValue == null)
            throw new IllegalArgumentException("fromValue와 toValue는 필수입니다.");

        ProjectValueMapping mapping = new ProjectValueMapping();
        mapping.project = project;
        mapping.fromValue = fromValue.trim();
        mapping.toValue = toValue.trim();
        return mapping;
    }

    /** 표시값(toValue)만 갱신 (동일 fromValue로 delete+insert 시 UK 위반 방지) */
    public void updateToValue(String toValue) {
        if (toValue == null || toValue.isBlank())
            throw new IllegalArgumentException("toValue는 필수입니다.");
        this.toValue = toValue.trim();
    }
}
