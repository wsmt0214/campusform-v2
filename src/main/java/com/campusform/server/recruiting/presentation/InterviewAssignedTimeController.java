package com.campusform.server.recruiting.presentation;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.recruiting.application.dto.response.InterviewAssignedTimeResponse;
import com.campusform.server.recruiting.application.service.InterviewAssignmentQueryService;

import lombok.RequiredArgsConstructor;

/**
 * 최종 면접시간 조회 API 컨트롤러
 * 
 * Manual 우선 + Auto fallback 규칙을 적용한 최종 면접시간을 반환합니다.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/interview/assigned-times")
@RequiredArgsConstructor
public class InterviewAssignedTimeController {

    private final InterviewAssignmentQueryService queryService;
    private final AuthService authService;

    /**
     * 프로젝트 내 전체 지원자의 최종 면접시간 조회
     */
    @GetMapping
    public ResponseEntity<List<InterviewAssignedTimeResponse>> getAssignedTimes(
            @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        List<InterviewAssignedTimeResponse> responses = queryService.getAssignedTimes(projectId, userId);
        return ResponseEntity.ok(responses);
    }
}

