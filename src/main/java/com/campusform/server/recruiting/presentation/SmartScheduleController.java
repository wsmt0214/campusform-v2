package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.recruiting.application.dto.response.SmartScheduleResponse;
import com.campusform.server.recruiting.application.service.SmartScheduleService;

import lombok.RequiredArgsConstructor;

/**
 * 스마트 시간표 API 컨트롤러
 */
@RestController
@RequestMapping("/api/projects/{projectId}/interview/smart-schedule")
@RequiredArgsConstructor
public class SmartScheduleController {

    private final SmartScheduleService smartScheduleService;
    private final AuthService authService;

    /**
     * 스마트 시간표 미리보기 (GET)
     */
    @GetMapping
    public ResponseEntity<SmartScheduleResponse> previewSchedule(@PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        SmartScheduleResponse response = smartScheduleService.generateSchedule(projectId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 스마트 시간표 생성 및 확정 (POST)
     */
    @PostMapping
    public ResponseEntity<SmartScheduleResponse> generateAndSaveSchedule(@PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        SmartScheduleResponse response = smartScheduleService.generateAndSaveSchedule(projectId, userId);
        return ResponseEntity.ok(response);
    }
}
