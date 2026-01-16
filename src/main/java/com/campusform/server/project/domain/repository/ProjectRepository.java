package com.campusform.server.project.domain.repository;

import java.util.Optional;

import com.campusform.server.project.domain.model.setting.Project;

/**
 * 도메인 계층의 infrastructure 인터페이스
 * 
 * 특정 기술에 의존하지 않고 도메인 관점에서 인터페이스를 서술합니다.
 * 
 * 따라서 Repository를 사용할 때 본 인터페이스를 사용합니다.
 */
public interface ProjectRepository {

    void save(Project project);

    Optional<Project> findById(Long id);

    Optional<Project> findBySheetUrl(String sheetUrl);
}
