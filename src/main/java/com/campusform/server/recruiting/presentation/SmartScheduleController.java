package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.campusform.server.global.security.CurrentUserId;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse;
import com.campusform.server.recruiting.application.service.SmartScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 스마트 시간표 API 컨트롤러
 */
@Tag(name = "스마트 시간표", description = "지원자와 면접관의 가능 시간을 바탕으로 최적의 면접 시간표를 자동 생성하는 API")
@RestController
@RequestMapping("/api/projects/{projectId}/interview/smart-schedule")
@RequiredArgsConstructor
public class SmartScheduleController {

    private final SmartScheduleService smartScheduleService;

    @Operation(summary = "스마트 시간표 생성 미리보기", description = "현재까지 수집된 정보를 바탕으로 스마트 시간표를 생성했을 때의 결과를 미리보기로 조회합니다. (저장되지 않음)")
    @GetMapping
    public ResponseEntity<SmartScheduleResponse> previewSchedule(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @CurrentUserId Long userId) {
        SmartScheduleResponse response = smartScheduleService.generateSchedule(projectId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "스마트 시간표 생성 및 확정", description = "스마트 시간표를 생성하고, 그 결과를 최종 확정하여 저장합니다.")
    @PostMapping
    public ResponseEntity<SmartScheduleResponse> generateAndSaveSchedule(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @CurrentUserId Long userId) {
        SmartScheduleResponse response = smartScheduleService.generateAndSaveSchedule(projectId, userId);
        return ResponseEntity.ok(response);
    }
}
