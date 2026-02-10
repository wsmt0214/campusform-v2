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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 최종 면접시간 조회 API 컨트롤러
 * 
 * Manual 우선 + Auto fallback 규칙을 적용한 최종 면접시간을 반환합니다.
 */
@Tag(name = "면접 시간 조회", description = "수동/자동 배정을 모두 고려한 최종 면접 시간 조회 API")
@RestController
@RequestMapping("/api/projects/{projectId}/interview/assigned-times")
@RequiredArgsConstructor
public class InterviewAssignedTimeController {

    private final InterviewAssignmentQueryService queryService;
    private final AuthService authService;

    @Operation(summary = "전체 지원자 최종 면접시간 조회", description = "프로젝트의 모든 지원자에게 배정된 최종 면접 시간을 조회합니다. 서류 지원자 관리 페이지에서 사용합니다.")
    @GetMapping
    public ResponseEntity<List<InterviewAssignedTimeResponse>> getAssignedTimes(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        List<InterviewAssignedTimeResponse> responses = queryService.getAssignedTimes(projectId, userId);
        return ResponseEntity.ok(responses);
    }
}
