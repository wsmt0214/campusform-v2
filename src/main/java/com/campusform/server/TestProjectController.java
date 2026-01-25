package com.campusform.server;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.project.application.dto.response.ProjectResponse;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.model.setting.value.ProjectState;
import com.campusform.server.project.domain.repository.ProjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 테스트용 프로젝트 컨트롤러
 * 
 * 프로젝트 상태를 직접 설정할 수 있는 테스트용 API를 제공합니다.
 * 프로덕션 환경에서는 비활성화하거나 삭제해야 합니다.
 */
@Profile("temporary") // API 테스트 환경에서만 활성화
@RestController
@RequestMapping("/api/test/projects")
@RequiredArgsConstructor
public class TestProjectController {

    private final ProjectRepository projectRepository;

    /**
     * 프로젝트 상태 설정 (테스트용)
     * 
     * 프로젝트 ID와 상태를 입력받아 프로젝트 상태를 직접 설정합니다.
     * 
     * 사용 가능한 상태:
     * - DOCUMENT_OPEN: 프로젝트 생성 완료, 서류 단계
     * - DOCUMENT_LOCKED: 서류 마감
     * - DOCUMENT_DONE: 서류 완료 및 프로젝트 종료
     * - INTERVIEW_OPEN: 면접 진행
     * - INTERVIEW_LOCKED: 면접 마감
     * - ALL_COMPLETE: 면접 종료 및 프로젝트 종료
     * 
     * 예시 요청 메시지:
     * PATCH /api/test/projects/1/state?state=INTERVIEW_LOCKED
     */
    @Transactional
    @PatchMapping("/{projectId}/state")
    public ResponseEntity<ProjectResponse> setProjectState(
            @PathVariable Long projectId,
            @RequestParam ProjectState state) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        project.setStateForTest(state);

        return ResponseEntity.ok(ProjectResponse.from(project));
    }
}
