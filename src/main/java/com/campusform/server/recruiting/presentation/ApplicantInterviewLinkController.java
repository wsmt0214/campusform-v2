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
import com.campusform.server.recruiting.application.service.ApplicantInterviewLinkService;

import lombok.RequiredArgsConstructor;

/**
 * 스마트 시간표 설정 - 3. 지원자 면접 가능 시간 모집 (Owner용)
 */
@RestController
@RequestMapping("/api/recruiting/projects")
@RequiredArgsConstructor
public class ApplicantInterviewLinkController {

    private final ApplicantInterviewLinkService applicantInterviewLinkService;
    private final AuthService authService;

    /**
     * 지원자 링크 조회
     * 
     * <pre>
     * 응답 예시:
     * {
     *   "token": "550e8400-e29b-41d4-a716-446655440000",
     *   "url": "/submit?token=550e8400-e29b-41d4-a716-446655440000"
     * }
     * </pre>
     */
    @GetMapping("/{projectId}/investigation-link")
    public ResponseEntity<ApplicantInterviewLinkResponse> getApplicantLink(
            @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        ApplicantInterviewLinkResponse response = applicantInterviewLinkService.getApplicantLink(projectId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 지원자 페이지 설정 조회
     * 
     * <pre>
     * 응답 예시:
     * {
     *   "enabled": true,
     *   "guidanceText": "면접 가능 시간을 선택해주세요."
     * }
     * </pre>
     */
    @GetMapping("/{projectId}/investigation-link/config")
    public ResponseEntity<ApplicantInterviewLinkConfigResponse> getApplicantLinkConfig(
            @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        ApplicantInterviewLinkConfigResponse response = applicantInterviewLinkService
                .getApplicantLinkConfig(projectId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 지원자 페이지 설정 수정
     * 
     * <pre>
     * 요청 예시:
     * {
     *   "enabled": true,
     *   "guidanceText": "면접 가능 시간을 선택해주세요."
     * }
     * </pre>
     */
    @PutMapping("/{projectId}/investigation-link/config")
    public ResponseEntity<ApplicantInterviewLinkConfigResponse> updateApplicantLinkConfig(
            @PathVariable Long projectId,
            Authentication authentication,
            @RequestBody UpdateApplicantLinkConfigRequest request) {
        Long userId = authService.extractUserId(authentication);
        ApplicantInterviewLinkConfigResponse response = applicantInterviewLinkService
                .updateApplicantLinkConfig(projectId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 면접 슬롯 목록 조회
     * 
     * <pre>
     * 응답 예시:
     * {
     *   "summaries": [
     *     {
     *       "date": "2024-07-01",
     *       "slots": [
     *         {
     *           "startTime": "10:00",
     *           "endTime": "10:20",
     *           "availableInterviewerCount": 2
     *         },
     *         {
     *           "startTime": "10:25",
     *           "endTime": "10:45",
     *           "availableInterviewerCount": 1
     *         }
     *       ]
     *     }
     *   ]
     * }
     * </pre>
     */
    @GetMapping("/{projectId}/interview-slots")
    public ResponseEntity<InterviewSlotListResponse> getInterviewSlotList(
            @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        InterviewSlotListResponse response = applicantInterviewLinkService.getInterviewSlotList(projectId, userId);
        return ResponseEntity.ok(response);
    }
}
