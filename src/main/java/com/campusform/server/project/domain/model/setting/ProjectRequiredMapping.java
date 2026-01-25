package com.campusform.server.project.domain.model.setting;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.campusform.server.project.domain.model.setting.value.RequiredFieldMapping;

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
 * 프로젝트 필수 필드 매핑 Entity
 * 
 * 스프레드시트의 컬럼 인덱스와 필수 필드(이름, 이메일 등)를 매핑합니다.
 */
@Entity
@Table(name = "project_mappings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProjectRequiredMapping {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    private Integer nameIdx;
    private Integer schoolIdx;
    private Integer majorIdx;
    private Integer genderIdx;
    private Integer phoneIdx;
    private Integer emailIdx;
    private Integer positionIdx;

    /**
     * 매핑 정보 생성 팩토리 메서드
     */
    public static ProjectRequiredMapping create(Project project, RequiredFieldMapping mappingValue) {
        ProjectRequiredMapping mapping = new ProjectRequiredMapping();
        mapping.project = project;
        mapping.nameIdx = mappingValue.getNameIdx();
        mapping.schoolIdx = mappingValue.getSchoolIdx();
        mapping.majorIdx = mappingValue.getMajorIdx();
        mapping.genderIdx = mappingValue.getGenderIdx();
        mapping.phoneIdx = mappingValue.getPhoneIdx();
        mapping.emailIdx = mappingValue.getEmailIdx();
        mapping.positionIdx = mappingValue.getPositionIdx();
        return mapping;
    }
}
