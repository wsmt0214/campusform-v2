package com.campusform.server.recruiting.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.project.application.dto.response.ProjectResponse;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;

import lombok.RequiredArgsConstructor;

/**
 * Recruiting(모집) 컨텍스트에서 "단계 종료/프로젝트 종료"를 담당하는 서비스
 *
 * 컨텍스트 기준:
 * - Project: 프로젝트 생성/설정(모집 공고의 설정값) 중심
 * - Recruiting: 모집 프로세스(서류/면접 진행, 마감, 결과 확정, 종료) 중심
 *
 * 따라서 "서류 종료", "면접 종료(전체 종료)" 같은 단계 전환은 Recruiting 쪽에서 오케스트레이션합니다.
 */
@Service
@RequiredArgsConstructor
public class RecruitingStageService {

    private final ProjectRepository projectRepository;

    /**
     * 서류 단계 종료 및 프로젝트 종료
     *
     * 전제:
     * - Project 엔티티의 상태가 DOCUMENT_LOCKED 여야 합니다.
     * - 요청한 사용자가 프로젝트의 OWNER여야 합니다.
     */
    @Transactional
    public ProjectResponse completeDocument(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        // 도메인 규칙(상태 전환 가능 여부 및 OWNER 검증)은 Project 엔티티 메서드가 책임집니다.
        project.completeDocument(userId);

        return ProjectResponse.from(project);
    }

    /**
     * 면접 단계 종료 및 프로젝트 종료(전체 종료)
     *
     * 전제:
     * - Project 엔티티의 상태가 INTERVIEW_LOCKED 여야 합니다.
     * - 요청한 사용자가 프로젝트의 OWNER여야 합니다.
     */
    @Transactional
    public ProjectResponse completeAll(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        // 도메인 규칙(상태 전환 가능 여부 및 OWNER 검증)은 Project 엔티티 메서드가 책임집니다.
        project.completeAll(userId);

        return ProjectResponse.from(project);
    }
}
