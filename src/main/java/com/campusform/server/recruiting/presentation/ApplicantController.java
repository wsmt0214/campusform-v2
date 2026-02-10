package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.recruiting.application.dto.request.ApplicantStatusUpdateRequest;
import com.campusform.server.recruiting.application.dto.response.ApplicantDetailResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantListResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantStatusUpdateResponse;
import com.campusform.server.recruiting.application.service.ApplicantService;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "지원자 관리", description = "지원자 목록 조회, 상태 변경, 상세 정보 등 API")
@RestController
@RequestMapping("/api/projects/{projectId}/applicants") // 공통 URL
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantService applicantService;
    private final AuthService authService;

    /**
     * 지원자 목록 조회
     */
    @Operation(summary = "지원자 목록 조회", description = "프로젝트의 전체 지원자 목록을 정렬하여 조회합니다.")
    @GetMapping
    public ResponseEntity<ApplicantListResponse> getApplicants(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "정렬 기준 (name: 이름순, latest: 최신순(기본값))", example = "latest") @RequestParam(required = false, defaultValue = "latest") String sort,
            @Parameter(description = "조회할 모집 단계") @RequestParam RecruitmentStage stage) {

        ApplicantListResponse response = applicantService.getApplicants(projectId, sort, stage);
        return ResponseEntity.ok(response);
    }

    /**
     * 지원자 상태변경 (보류/합격/불합격)
     */
    @Operation(summary = "지원자 상태 변경", description = "특정 지원자의 서류/면접 전형 상태를 변경합니다. (예: 보류, 합격, 불합격)")
    @PatchMapping("/{applicantId}")
    public ResponseEntity<ApplicantStatusUpdateResponse> updateStatus(
            @Parameter(description = "지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "변경할 모집 단계") @RequestParam RecruitmentStage stage, // ?stage=DOCUMENT
            @RequestBody ApplicantStatusUpdateRequest request // Body { "status": "PASS" }
    ) {
        ApplicantStatusUpdateResponse response = applicantService.updateApplicantStatus(
                applicantId,
                stage,
                request.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * 즐겨찾기
     */
    @Operation(summary = "즐겨찾기", description = "특정 지원자를 찜하거나 찜을 해제합니다.")
    @PatchMapping("/{applicantId}/bookmark") // 또는 POST
    public ResponseEntity<Void> Bookmark(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "찜할 지원자 ID") @PathVariable Long applicantId) {
        // 서비스의 토글 메서드 호출
        applicantService.Bookmark(applicantId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 단계의 지원자 상세정보
     */
    @Operation(summary = "특정 단계의 지원자 상세 정보 조회", description = "특정 지원자의 상세 정보(인적사항, 답변, 댓글 등)를 조회합니다.")
    @GetMapping("/{applicantId}")
    public ResponseEntity<ApplicantDetailResponse> getApplicantDetail(
            @Parameter(description = "지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "조회할 모집 단계") @RequestParam RecruitmentStage stage) {
        ApplicantDetailResponse response = applicantService.getApplicantDetail(applicantId, stage);
        return ResponseEntity.ok(response);
    }
}