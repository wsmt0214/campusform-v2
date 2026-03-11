package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.campusform.server.global.security.CurrentUserId;
import com.campusform.server.recruiting.application.dto.request.interview.UpsertInterviewSettingRequest;
import com.campusform.server.recruiting.application.dto.response.interview.InterviewSettingResponse;
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

    @Operation(summary = "면접 정보 설정 조회", description = "프로젝트의 면접 기본 정보(기간, 시간, 슬롯 당 인원 등)를 조회합니다.")
    @GetMapping("/{projectId}/interview-setting")
    public ResponseEntity<InterviewSettingResponse> getInterviewSetting(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @CurrentUserId Long userId) {
        InterviewSettingResponse response = interviewSettingService.getSetting(projectId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "면접 정보 설정 저장/수정", description = "프로젝트의 면접 기본 정보를 저장하거나 수정합니다.")
    @PutMapping("/{projectId}/interview-setting")
    public ResponseEntity<InterviewSettingResponse> upsertInterviewSetting(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @CurrentUserId Long userId,
            @RequestBody UpsertInterviewSettingRequest request) {
        InterviewSettingResponse response = interviewSettingService.saveOrUpdateSetting(projectId, userId, request);
        return ResponseEntity.ok(response);
    }
}
