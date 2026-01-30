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

import lombok.RequiredArgsConstructor;

/**
 * 스마트 시간표 설정 - 1. 면접 정보 설정
 */
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
    @GetMapping("/{projectId}/interview-setting")
    public ResponseEntity<InterviewSettingResponse> getInterviewSetting(
            @PathVariable Long projectId,
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
    @PutMapping("/{projectId}/interview-setting")
    public ResponseEntity<InterviewSettingResponse> upsertInterviewSetting(
            @PathVariable Long projectId,
            Authentication authentication,
            @RequestBody UpsertInterviewSettingRequest request) {
        Long userId = authService.extractUserId(authentication);
        InterviewSettingResponse response = interviewSettingService.saveOrUpdateSetting(projectId, userId, request);
        return ResponseEntity.ok(response);
    }
}
