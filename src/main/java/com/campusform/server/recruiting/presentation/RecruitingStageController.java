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
 * 왜 Recruiting에 있나?
 * - "서류/면접 단계 마감 및 프로젝트 종료"는 모집 프로세스의 일부입니다.
 * - Project 컨텍스트는 프로젝트(모집 공고)의 설정/기본 정보에 집중합니다.
 */
@Tag(name = "모집 단계", description = "서류/면접 단계 마감 및 프로젝트 종료 API")
@RestController
@RequestMapping("/api/recruiting/projects")
@RequiredArgsConstructor
public class RecruitingStageController {

    private final RecruitingStageService recruitingStageService;
    private final AuthService authService;

    @Operation(summary = "서류 단계 마감", description = "서류 단계를 마감하고 프로젝트 상태를 '서류 완료'로 변경합니다. (소유자만 가능)")
    @PatchMapping("/{projectId}/complete-document")
    public ResponseEntity<ProjectResponse> completeDocument(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        ProjectResponse response = recruitingStageService.completeDocument(projectId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "면접 단계 마감 (프로젝트 전체 종료)", description = "면접 단계를 마감하고 프로젝트 전체를 종료합니다. (소유자만 가능)")
    @PatchMapping("/{projectId}/complete-all")
    public ResponseEntity<ProjectResponse> completeAll(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        ProjectResponse response = recruitingStageService.completeAll(projectId, userId);
        return ResponseEntity.ok(response);
    }
}
