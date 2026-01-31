package com.campusform.server.project.presentation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.project.application.dto.SpreadsheetColumnResponse;
import com.campusform.server.project.application.dto.response.SheetSyncResponse;
import com.campusform.server.project.application.service.SpreadsheetService;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;

import lombok.RequiredArgsConstructor;

/**
 * Google Sheets API 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class GoogleSheetController {

    private final SpreadsheetService spreadsheetService;
    private final ProjectRepository projectRepository;
    private final AuthService authService;

    /**
     * 시트 헤더 조회 API
     */
    @GetMapping("/sheet-headers")
    public ResponseEntity<List<SpreadsheetColumnResponse>> getSheetHeaders(
            @RequestParam String sheetUrl,
            Authentication authentication) {

        Long userId = authService.extractUserId(authentication);

        Project project = projectRepository.findBySheetUrl(sheetUrl)
                .orElseThrow(() -> new IllegalArgumentException("sheetUrl에 해당하는 프로젝트가 존재하지 않습니다."));
        project.validateAdminAccess(userId);

        // 시트 헤더 조회 (토큰은 GoogleSheetsServiceFactory에서 자동 갱신됨)
        List<SpreadsheetColumnResponse> headers = spreadsheetService.getSheetHeaders(sheetUrl, project.getOwnerId());
        return ResponseEntity.ok(headers);
    }

    /**
     * 시트 동기화 API
     */
    @PostMapping("/{projectId}/sync-sheet")
    public ResponseEntity<SheetSyncResponse> syncSheet(
            @PathVariable Long projectId,
            Authentication authentication) {

        Long userId = authService.extractUserId(authentication);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        // 프로젝트에 관한 관리자만 호출 가능
        project.validateAdminAccess(userId);

        // 시트 동기화 실행 (토큰은 GoogleSheetsServiceFactory에서 자동 갱신됨)
        try {
            int syncedCount = spreadsheetService.syncSheet(projectId);
            return ResponseEntity.ok(SheetSyncResponse.success(syncedCount));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SheetSyncResponse.failure("시트 동기화 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}