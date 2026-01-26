package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.project.application.dto.response.ProjectResponse;
import com.campusform.server.recruiting.application.service.RecruitingStageService;

import lombok.RequiredArgsConstructor;

/**
 * 모집(Recruiting) 컨텍스트의 "단계 종료" API
 */
@RestController
@RequestMapping("/api/recruiting/projects")
@RequiredArgsConstructor
public class RecruitingStageController {

    private final RecruitingStageService recruitingStageService;
    private final AuthService authService;

    /**
     * 서류 단계 종료 및 프로젝트 종료
     *
     * 전제:
     * - 프로젝트 상태가 DOCUMENT_LOCKED 인 경우에만 가능합니다.
     * - 요청한 사용자가 프로젝트의 OWNER여야 합니다.
     */
    @PatchMapping("/{projectId}/complete-document")
    public ResponseEntity<ProjectResponse> completeDocument(
            @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        ProjectResponse response = recruitingStageService.completeDocument(projectId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 면접 단계 종료 및 프로젝트 종료(전체 종료)
     *
     * 전제:
     * - 프로젝트 상태가 INTERVIEW_LOCKED 인 경우에만 가능합니다.
     * - 요청한 사용자가 프로젝트의 OWNER여야 합니다.
     */
    @PatchMapping("/{projectId}/complete-all")
    public ResponseEntity<ProjectResponse> completeAll(
            @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        ProjectResponse response = recruitingStageService.completeAll(projectId, userId);
        return ResponseEntity.ok(response);
    }
}
