package com.campusform.server.recruiting.presentation;

import com.campusform.server.recruiting.application.dto.request.ApplicantStatusUpdateRequest;
import com.campusform.server.recruiting.application.dto.response.ApplicantDetailResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantListResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantStatusUpdateResponse;
import com.campusform.server.recruiting.application.service.ApplicantService;
import com.campusform.server.recruiting.domain.model.applicant.value.StageStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "지원자 관리", description = "지원자 목록 조회, 상태 변경, 상세 정보 등 API")
@RestController
@RequestMapping("/api/projects/{projectId}/applicants") // 공통 URL
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantService applicantService;

    /**
     * 1. 지원자 목록 조회 (정렬 기능 포함)
     * GET /projects/1/applicants?sort=name (또는 sort=latest)
     */
    @Operation(summary = "지원자 목록 조회", description = "프로젝트의 전체 지원자 목록을 정렬하여 조회합니다.")
    @GetMapping
    public ResponseEntity<ApplicantListResponse> getApplicants(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "정렬 기준 (name: 이름순, latest: 최신순)", example = "latest") @RequestParam(required = false, defaultValue = "latest") String sort,
            @Parameter(description = "조회할 모집 단계 (미지정 시 전체)") @RequestParam(required = false) StageStatus stage
    ) {

        ApplicantListResponse response = applicantService.getApplicants(projectId, sort,stage);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. 지원자 상태변경 (보류/합격/불합격)
     */
    @Operation(summary = "지원자 상태 변경", description = "특정 지원자의 서류/면접 전형 상태를 변경합니다. (예: 보류, 서류 합격, 최종 합격 등)")
    @PatchMapping("/{applicantId}")
    public ResponseEntity<ApplicantStatusUpdateResponse> updateStatus(
            @Parameter(description = "지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "변경할 모집 단계") @RequestParam StageStatus stage, // ?stage=DOCUMENT
            @RequestBody ApplicantStatusUpdateRequest request // Body { "status": "PASS" }
    ) {
        ApplicantStatusUpdateResponse response = applicantService.updateApplicantStatus(
                applicantId,
                stage,
                request.getStatus()
        );

        return ResponseEntity.ok(response);
    }
    /**
     * 3. 찜하기 버튼 클릭 (토글)
     * PATCH /projects/1/applicants/{applicantId}/bookmark
     * (경로는 편한대로 설정, 보통 리소스 하위에 둠)
     */
    @Operation(summary = "지원자 찜하기 (토글)", description = "특정 지원자를 찜하거나 찜을 해제합니다.")
    @PatchMapping("/{applicantId}/bookmark") // 또는 POST
    public ResponseEntity<Void> Bookmark(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "찜할 지원자 ID") @PathVariable Long applicantId
    ) {
        // 서비스의 토글 메서드 호출
        applicantService.Bookmark(applicantId);
        return ResponseEntity.ok().build();
    }
    /**
     * 4. 지원자 상세정보
     *
     */
    @Operation(summary = "지원자 상세 정보 조회", description = "특정 지원자의 상세 정보(인적사항, 답변, 댓글 등)를 조회합니다.")
    @GetMapping("/{applicantId}")
    public ResponseEntity<ApplicantDetailResponse> getApplicantDetail(
            @Parameter(description = "지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "조회할 모집 단계") @RequestParam StageStatus stage
    ){
        ApplicantDetailResponse response = applicantService.getApplicantDetail(applicantId, stage);
        return ResponseEntity.ok(response);
    }
}