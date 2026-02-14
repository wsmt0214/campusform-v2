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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 모집(Recruiting) 컨텍스트의 "단계 종료" API
 *
 * 프로젝트 상태 흐름:
 * DOCUMENT → DOCUMENT_COMPLETE (면접 없이 종료)
 * DOCUMENT → INTERVIEW → INTERVIEW_COMPLETE (면접까지 진행 후 종료)
 *
 * DOCUMENT → INTERVIEW 전환은 PATCH /{projectId}/start-interview API로 수행합니다.
 */
@Tag(name = "모집 단계", description = "서류/면접 단계 전환 및 종료 API")
@RestController
@RequestMapping("/api/recruiting/projects")
@RequiredArgsConstructor
public class RecruitingStageController {

    private final RecruitingStageService recruitingStageService;
    private final AuthService authService;

    @Operation(summary = "면접 단계 시작", description = "서류 단계를 마치고 면접 단계로 전환합니다. DOCUMENT → INTERVIEW (소유자만 가능)")
    @PatchMapping("/{projectId}/start-interview")
    public ResponseEntity<ProjectResponse> startInterview(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        ProjectResponse response = recruitingStageService.startInterview(projectId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "서류 단계 종료", description = "서류 단계를 종료하고 프로젝트를 완료합니다. 면접 없이 종료 시 사용합니다. DOCUMENT → DOCUMENT_COMPLETE (소유자만 가능)")
    @PatchMapping("/{projectId}/complete-document")
    public ResponseEntity<ProjectResponse> completeDocument(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        ProjectResponse response = recruitingStageService.completeDocument(projectId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "면접 단계 종료 (프로젝트 전체 종료)", description = "면접 단계를 종료하고 프로젝트 전체를 완료합니다. INTERVIEW → INTERVIEW_COMPLETE (소유자만 가능)")
    @PatchMapping("/{projectId}/complete-all")
    public ResponseEntity<ProjectResponse> completeAll(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        ProjectResponse response = recruitingStageService.completeAll(projectId, userId);
        return ResponseEntity.ok(response);
    }
}
