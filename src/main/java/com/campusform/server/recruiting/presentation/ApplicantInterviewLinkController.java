package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.recruiting.application.dto.request.UpdateApplicantLinkConfigRequest;
import com.campusform.server.recruiting.application.dto.response.ApplicantInterviewLinkConfigResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantInterviewLinkResponse;
import com.campusform.server.recruiting.application.dto.response.InterviewSlotListResponse;
import com.campusform.server.recruiting.application.dto.response.SlotApplicantListResponse;
import com.campusform.server.recruiting.application.service.ApplicantInterviewLinkService;
import com.campusform.server.recruiting.application.service.SlotApplicantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 스마트 시간표 설정 - 3. 지원자 면접 가능 시간 모집 (Owner용)
 */
@Tag(name = "지원자 면접 링크", description = "지원자에게 발송할 면접 시간 제출 링크 및 관련 설정 API")
@RestController
@RequestMapping("/api/recruiting/projects")
@RequiredArgsConstructor
public class ApplicantInterviewLinkController {

    private final ApplicantInterviewLinkService applicantInterviewLinkService;
    private final SlotApplicantService slotApplicantService;

    private final AuthService authService;

    @Operation(summary = "지원자 면접 시간 제출 링크 조회", description = "지원자에게 배포할, 면접 가능 시간을 제출받는 페이지의 고유 링크(토큰)를 조회합니다.")
    @GetMapping("/{projectId}/investigation-link")
    public ResponseEntity<ApplicantInterviewLinkResponse> getApplicantLink(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        ApplicantInterviewLinkResponse response = applicantInterviewLinkService.getApplicantLink(projectId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "지원자 시간 제출 페이지 설정 조회", description = "지원자에게 보여질 면접 시간 제출 페이지의 활성화 여부 및 안내 문구를 조회합니다.")
    @GetMapping("/{projectId}/investigation-link/config")
    public ResponseEntity<ApplicantInterviewLinkConfigResponse> getApplicantLinkConfig(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        ApplicantInterviewLinkConfigResponse response = applicantInterviewLinkService
                .getApplicantLinkConfig(projectId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "지원자 시간 제출 페이지 설정 수정", description = "지원자에게 보여질 면접 시간 제출 페이지의 활성화 여부 및 안내 문구를 수정합니다.")
    @PutMapping("/{projectId}/investigation-link/config")
    public ResponseEntity<ApplicantInterviewLinkConfigResponse> updateApplicantLinkConfig(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication,
            @RequestBody UpdateApplicantLinkConfigRequest request) {
        Long userId = authService.extractUserId(authentication);
        ApplicantInterviewLinkConfigResponse response = applicantInterviewLinkService
                .updateApplicantLinkConfig(projectId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "관리자용 전체 면접 슬롯 목록 조회", description = "면접관들이 제출한 시간을 바탕으로 생성된 전체 면접 슬롯 목록과, 각 슬롯별 참여 가능 면접관 수를 함께 조회합니다.")
    @GetMapping("/{projectId}/interview-slots")
    public ResponseEntity<InterviewSlotListResponse> getInterviewSlotList(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        InterviewSlotListResponse response = applicantInterviewLinkService.getInterviewSlotList(projectId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "슬롯별 신청 지원자 목록 조회", description = "정의된 모든 면접 슬롯 별로, 해당 슬롯을 신청한 지원자들의 목록을 전체 조회합니다.")
    @GetMapping("/{projectId}/interview-slots/applicants")
    public ResponseEntity<SlotApplicantListResponse> getAllApplicantsBySlots(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);

        SlotApplicantListResponse response = slotApplicantService.getAllApplicantsBySlots(projectId, userId);
        return ResponseEntity.ok(response);
    }
}
