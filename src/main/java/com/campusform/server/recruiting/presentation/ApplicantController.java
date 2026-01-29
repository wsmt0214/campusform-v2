package com.campusform.server.recruiting.presentation;

import com.campusform.server.recruiting.application.dto.request.ApplicantStatusUpdateRequest;
import com.campusform.server.recruiting.application.dto.response.ApplicantDetailResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantListResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantStatusUpdateResponse;
import com.campusform.server.recruiting.application.service.ApplicantService;
import com.campusform.server.recruiting.domain.model.applicant.value.StageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/applicants") // 공통 URL
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantService applicantService;

    /**
     * 1. 지원자 목록 조회 (정렬 기능 포함)
     * GET /projects/1/applicants?sort=name (또는 sort=latest)
     */
    @GetMapping
    public ResponseEntity<ApplicantListResponse> getApplicants(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false) StageStatus stage
    ) {

        ApplicantListResponse response = applicantService.getApplicants(projectId, sort,stage);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. 지원자 상태변경 (보류/합격/불합격)
     */
    @PatchMapping("/{applicantId}")
    public ResponseEntity<ApplicantStatusUpdateResponse> updateStatus(
            @PathVariable Long applicantId,
            @RequestParam StageStatus stage, // ?stage=DOCUMENT
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
    @PatchMapping("/{applicantId}/bookmark") // 또는 POST
    public ResponseEntity<Void> Bookmark(
            //@PathVariable Long projectId, // URL 구조상 받을 수도 있고 안 쓸 수도 있음
            @PathVariable Long applicantId
    ) {
        // 서비스의 토글 메서드 호출
        applicantService.Bookmark(applicantId);
        return ResponseEntity.ok().build();
    }
    /**
     * 4. 지원자 상세정보
     *
     */
    @GetMapping("/{applicantId}")
    public ResponseEntity<ApplicantDetailResponse> getApplicantDetail(
            @PathVariable Long applicantId,
            @RequestParam StageStatus stage
    ){
        ApplicantDetailResponse response = applicantService.getApplicantDetail(applicantId, stage);
        return ResponseEntity.ok(response);
    }
}