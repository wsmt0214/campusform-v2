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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 테스트용 프로젝트 컨트롤러
 * 
 * 프로젝트 상태를 직접 설정할 수 있는 테스트용 API를 제공합니다.
 * 프로덕션 환경에서는 비활성화하거나 삭제해야 합니다.
 */
@Profile("local") // API 테스트 환경에서만 활성화
@Tag(name = "테스트", description = "개발 및 테스트용 API")
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
     * - DOCUMENT: 서류 심사 진행 중
     * - INTERVIEW: 면접 진행 중
     * - DOCUMENT_COMPLETE: 서류 심사 완료 (면접 없이 종료)
     * - INTERVIEW_COMPLETE: 면접 완료 (전체 종료)
     * 
     * 예시 요청 메시지:
     * PATCH /api/test/projects/1/state?state=INTERVIEW
     */
    @Transactional
    @Operation(summary = "프로젝트 상태 강제 변경 (테스트)", description = "특정 프로젝트의 진행 상태를 강제로 변경합니다. `temporary` 프로필에서만 활성화됩니다.", security = {})
    @PatchMapping("/{projectId}/state")
    public ResponseEntity<ProjectResponse> setProjectState(
            @Parameter(description = "상태를 변경할 프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "변경할 프로젝트 상태") @RequestParam ProjectState state) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        project.setStateForTest(state);

        return ResponseEntity.ok(ProjectResponse.from(project));
    }
}
