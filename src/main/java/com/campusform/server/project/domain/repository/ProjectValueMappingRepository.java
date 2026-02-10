package com.campusform.server.project.domain.repository;

import java.util.List;

import com.campusform.server.project.domain.model.setting.ProjectValueMapping;

/**
 * 프로젝트 값 치환 규칙 Repository 인터페이스
 *
 * projectId 기준으로 치환 규칙 조회·삭제를 제공합니다.
 */
public interface ProjectValueMappingRepository {

    /**
     * 프로젝트에 등록된 값 치환 규칙 목록 조회
     *
     * @param projectId 프로젝트 ID
     * @return 치환 규칙 목록 (없으면 빈 리스트)
     */
    List<ProjectValueMapping> findByProjectId(Long projectId);

    /**
     * 프로젝트의 값 치환 규칙 전체 삭제 (편집 시 전체 교체용)
     *
     * @param projectId 프로젝트 ID
     */
    void deleteByProjectId(Long projectId);
}
