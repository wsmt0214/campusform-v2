package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
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

    @Operation(
            summary = "스마트 시간표 생성",
            description = "수집된 정보로 알고리즘을 실행해 결과만 반환합니다. DB에 저장하지 않습니다.")
    @PostMapping("/generate")
    public ResponseEntity<SmartScheduleResponse> generateSchedule(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @CurrentUserId Long userId) {
        SmartScheduleResponse response = smartScheduleService.generateSchedule(projectId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "스마트 시간표 확정",
            description = "알고리즘을 다시 실행한 뒤 그 결과를 DB에 저장하여 확정합니다. (입력이 동일하면 생성 API와 같은 배정이 나옵니다.)")
    @PostMapping("/confirm")
    public ResponseEntity<SmartScheduleResponse> confirmSchedule(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @CurrentUserId Long userId) {
        SmartScheduleResponse response = smartScheduleService.generateAndSaveSchedule(projectId, userId);
        return ResponseEntity.ok(response);
    }
}
