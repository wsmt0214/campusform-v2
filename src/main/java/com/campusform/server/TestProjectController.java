package com.campusform.server;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.project.application.dto.response.ProjectResponse;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.model.setting.value.ProjectState;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.application.dto.response.InterviewTestDataGenerateResponse;
import com.campusform.server.recruiting.application.service.InterviewTestDataService;

import io.swagger.v3.oas.annotations.Hidden;
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
@Hidden
@Tag(name = "테스트", description = "개발 및 테스트용 API")
@RestController
@RequestMapping("/api/test/projects")
@RequiredArgsConstructor
public class TestProjectController {

    private final ProjectRepository projectRepository;
    private final InterviewTestDataService interviewTestDataService;

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

    /**
     * 면접 테스트 데이터 자동 생성 (테스트용)
     *
     * 면접관 가용 시간(30분 블록)과 지원자 슬롯 선택을 자동으로 생성합니다.
     * 면접 정보 설정이 완료된 프로젝트에서만 사용 가능합니다.
     */
    /**
     * 면접 테스트 데이터 자동 생성 (테스트)
     * 
     * API 요청 방법:
     * POST 방식으로 /api/test/projects/{projectId}/generate-interview-data 엔드포인트를 호출하면
     * 됩니다.
     * 
     * 요청 경로
     * - {projectId}에는 면접 데이터를 생성할 프로젝트의 ID를 입력합니다. (예:
     * /api/test/projects/12/generate-interview-data)
     * 
     * 요청 파라미터 (Query String)
     * - dayParticipationRate: 면접관이 날짜별로 참여할 확률을 0.0~1.0 사이의 실수로 입력합니다. 생략 시 기본값은
     * 0.7 입니다. (예: ?dayParticipationRate=0.6)
     * - slotSelectionRate: 지원자가 각 시간 슬롯을 선택할 확률을 0.0~1.0 사이의 실수로 입력합니다. 생략 시 기본값은
     * 0.3 입니다. (예: ?slotSelectionRate=0.5)
     * 
     * 예시 요청
     * POST
     * /api/test/projects/12/generate-interview-data?dayParticipationRate=0.8&slotSelectionRate=0.4
     * 
     * 설명
     * 해당 API는 지정한 프로젝트에 대해
     * 1) 면접관별 날짜 참여 확률에 따라 가용 시간 블록(30분 단위)을 무작위로 생성하고,
     * 2) 지원자별로 시간 슬롯 선택 확률에 따라 면접 시간 슬롯을 랜덤 생성합니다.
     * 기존에 등록된 면접관 가용 블록과 지원자 슬롯 데이터는 모두 삭제되고 새롭게 생성됩니다.
     * 프로덕션 환경에서는 사용할 수 없으며, 테스트 데이터 자동 구성을 위해 사용합니다.
     */

    @Operation(summary = "면접 테스트 데이터 자동 생성 (테스트)", description = "면접관 가용 시간과 지원자 슬롯 선택을 확률 기반으로 자동 생성합니다. 기존 데이터는 삭제 후 재생성됩니다.", security = {})
    @PostMapping("/{projectId}/generate-interview-data")
    public ResponseEntity<InterviewTestDataGenerateResponse> generateInterviewTestData(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "면접관 날짜 별 참여 확률 (0.0~1.0, 기본값 0.7)") @RequestParam(defaultValue = "0.7") double dayParticipationRate,
            @Parameter(description = "지원자 슬롯 별 선택 확률 (0.0~1.0, 기본값 0.3)") @RequestParam(defaultValue = "0.3") double slotSelectionRate) {
        InterviewTestDataGenerateResponse result = interviewTestDataService.generateTestData(projectId,
                dayParticipationRate, slotSelectionRate);
        return ResponseEntity.ok(result);
    }
}
