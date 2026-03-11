package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.recruiting.application.dto.request.applicant.ApplicantStatusUpdateRequest;
import com.campusform.server.recruiting.application.dto.response.applicant.ApplicantDetailResponse;
import com.campusform.server.recruiting.application.dto.response.applicant.ApplicantListResponse;
import com.campusform.server.recruiting.application.dto.response.applicant.ApplicantStatusUpdateResponse;
import com.campusform.server.recruiting.application.service.ApplicantCommandService;
import com.campusform.server.recruiting.application.service.ApplicantQueryService;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 지원자 관리 API
 */
@Tag(name = "지원자 관리", description = "지원자 목록 조회, 상태 변경, 상세 정보 등 API")
@RestController
@RequestMapping("/api/projects/{projectId}/applicants")
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantQueryService applicantQueryService;
    private final ApplicantCommandService applicantCommandService;
    private final AuthService authService;

    /**
     * 프로젝트의 지원자 목록을 모집 단계에 따라 조회
     */
    @Operation(summary = "지원자 목록 조회", description = "프로젝트 ID에 해당하는 지원자 목록을 모집 단계(stage)별로 조회합니다. "
            + "서류·면접 단계별 지원자 현황(전체, 합격, 불합격, 보류 수)과 각 지원자의 간략 정보가 포함됩니다.")
    @GetMapping
    public ResponseEntity<ApplicantListResponse> getApplicants(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "조회할 모집 단계") @RequestParam RecruitmentStage stage,
            Authentication authentication) {

        Long userId = authService.extractUserId(authentication);
        ApplicantListResponse response = applicantQueryService.getApplicants(projectId, stage, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "지원자 상태 변경", description = "특정 지원자의 서류/면접 전형 상태를 변경합니다. (예: 보류, 합격, 불합격)")
    @PatchMapping("/{applicantId}")
    public ResponseEntity<ApplicantStatusUpdateResponse> updateStatus(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "변경할 모집 단계") @RequestParam RecruitmentStage stage, // ?stage=DOCUMENT
            @RequestBody ApplicantStatusUpdateRequest request, // Body { "status": "PASS" }
            Authentication authentication
    ) {
        Long userId = authService.extractUserId(authentication);
        ApplicantStatusUpdateResponse response = applicantCommandService.updateApplicantStatus(
                projectId,
                applicantId,
                stage,
                request.getStatus(),
                userId);

        return ResponseEntity.ok(response);
    }

    /** 즐겨찾기 토글 */
    @Operation(summary = "즐겨찾기", description = "특정 지원자를 서류/면접 단계별로 찜하거나 찜을 해제합니다.")
    @PatchMapping("/{applicantId}/bookmark") // 또는 POST
    public ResponseEntity<Void> Bookmark(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "찜할 지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "즐겨찾기를 토글할 모집 단계 (DOCUMENT: 서류, INTERVIEW: 면접)") @RequestParam RecruitmentStage stage,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        applicantCommandService.toggleBookmark(projectId, applicantId, stage, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 단계의 지원자 상세정보
     *
     * 응답 예시:
     * 
     * <pre>
     * {
     *   "applicantId": 1,
     *   "name": "홍길동",
     *   "gender": "MALE",
     *   "school": "캠퍼스대학교",
     *   "major": "컴퓨터공학과",
     *   "position": "백엔드",
     *   "phoneNumber": "010-1234-5678",
     *   "email": "applicant@example.com",
     *   "status": "PASS",
     *   "isFavorite": true,
     *   "commentCount": 3,
     *   "answers": [
     *     {
     *       "question": "자기소개를 해주세요.",
     *       "answer": "안녕하세요, 저는..."
     *     }
     *   ]
     * }
     * </pre>
     */
    @Operation(summary = "특정 단계의 지원자 상세 정보 조회", description = "특정 지원자의 상세 정보(인적사항, 답변, 댓글, 면접 시간 배정 등)를 조회합니다.")
    @GetMapping("/{applicantId}")
    public ResponseEntity<ApplicantDetailResponse> getApplicantDetail(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "조회할 모집 단계") @RequestParam RecruitmentStage stage,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        ApplicantDetailResponse response = applicantQueryService.getApplicantDetail(projectId, applicantId, stage, userId);
        return ResponseEntity.ok(response);
    }
}
