package com.campusform.server.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campusform.server.project.domain.exception.ProjectAccessDeniedException;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 단위 권한 검증을 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
public class ProjectAuthorizationService {

    private final ProjectRepository projectRepository;

    /**
     * 해당 프로젝트에 대한 관리자(OWNER 또는 ADMIN) 권한이 있는지 검증
     *
     * @throws ProjectAccessDeniedException 권한이 없는 경우
     */
    @Transactional(readOnly = true)
    public void assertAdmin(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new ProjectAccessDeniedException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        project.validateAdminAccess(userId);
    }
}

