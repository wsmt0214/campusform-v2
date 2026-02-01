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
import com.campusform.server.recruiting.application.dto.request.UpsertInterviewSettingRequest;
import com.campusform.server.recruiting.application.dto.response.InterviewSettingResponse;
import com.campusform.server.recruiting.application.service.InterviewSettingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 스마트 시간표 설정 - 1. 면접 정보 설정
 */
@Tag(name = "면접 설정", description = "스마트 시간표 생성을 위한 기본 면접 정보 설정 API")
@RestController
@RequestMapping("/api/recruiting/projects")
@RequiredArgsConstructor
public class InterviewSettingController {

    private final InterviewSettingService interviewSettingService;
    private final AuthService authService;

    /**
     * 면접 정보 설정 조회
     * 
     * 응답 예시:
     * {
     * "configured": true,
     * "startDate": "2024-08-01",
     * "endDate": "2024-08-05",
     * "startTime": "10:00",
     * "endTime": "18:00",
     * "maxApplicantsPerSlot": 3,
     * "minInterviewersPerSlot": 2,
     * "maxInterviewersPerSlot": 3,
     * "slotDurationMin": 20,
     * "slotBreakMin": 5,
     * "investigationLinkToken": "공개 링크 토큰"
     * }
     */
    @Operation(summary = "면접 정보 설정 조회", description = "프로젝트의 면접 기본 정보(기간, 시간, 슬롯 당 인원 등)를 조회합니다.")
    @GetMapping("/{projectId}/interview-setting")
    public ResponseEntity<InterviewSettingResponse> getInterviewSetting(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        InterviewSettingResponse response = interviewSettingService.getSetting(projectId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 면접 정보 설정 저장/수정 ("설정하기" 버튼)
     *
     * 요청 예시
     * {
     * "startDate": "2024-08-01",
     * "endDate": "2024-08-05",
     * "startTime": "10:00",
     * "endTime": "18:00",
     * "maxApplicantsPerSlot": 3,
     * "minInterviewersPerSlot": 2,
     * "maxInterviewersPerSlot": 3,
     * "slotDurationMin": 20,
     * "slotBreakMin": 5
     * }
     */
    @Operation(summary = "면접 정보 설정 저장/수정", description = "프로젝트의 면접 기본 정보를 저장하거나 수정합니다.")
    @PutMapping("/{projectId}/interview-setting")
    public ResponseEntity<InterviewSettingResponse> upsertInterviewSetting(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication,
            @RequestBody UpsertInterviewSettingRequest request) {
        Long userId = authService.extractUserId(authentication);
        InterviewSettingResponse response = interviewSettingService.saveOrUpdateSetting(projectId, userId, request);
        return ResponseEntity.ok(response);
    }
}
