package com.campusform.server.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campusform.server.project.domain.exception.ProjectNotFoundException;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 접근 제어 관련 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectAccessService {

    private final ProjectRepository projectRepository;

    public Project loadProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    /**
     * 프로젝트를 조회하고 Admin 권한을 검증 
     */
    public Project getProjectWithAdminAccess(Long projectId, Long userId) {
        Project project = loadProject(projectId);
        project.validateAdminAccess(userId);
        return project;
    }

    /**
     * 프로젝트를 조회하고 Owner 권한을 검증 
     */
    public Project getProjectWithOwnerAccess(Long projectId, Long userId) {
        Project project = loadProject(projectId);
        project.validateOwnerAccess(userId);
        return project;
    }
}
