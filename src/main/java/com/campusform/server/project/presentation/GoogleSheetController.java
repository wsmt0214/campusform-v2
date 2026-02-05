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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Google Sheets API 관련 API 컨트롤러
 */
@Tag(name = "Google 연동", description = "Google OAuth2 및 Sheets 연동 관련 API")
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
    @Operation(summary = "Google Sheet 헤더(컬럼) 조회", description = "주어진 Google Sheet URL의 첫 번째 행(헤더)에 있는 컬럼 목록을 조회합니다.")
    @GetMapping("/sheet-headers")
    public ResponseEntity<List<SpreadsheetColumnResponse>> getSheetHeaders(
            @Parameter(description = "헤더를 조회할 Google Sheet의 전체 URL", required = true) @RequestParam String sheetUrl,
            Authentication authentication) {

        Long userId = authService.extractUserId(authentication);

        // 시트 헤더 조회 (토큰은 GoogleSheetsServiceFactory에서 자동 갱신됨)
        List<SpreadsheetColumnResponse> headers = spreadsheetService.getSheetHeaders(sheetUrl, userId);
        return ResponseEntity.ok(headers);
    }

    /**
     * 시트 동기화 API
     */
    @Operation(summary = "Google Sheet와 지원자 정보 동기화", description = "프로젝트에 연결된 Google Sheet의 데이터를 읽어와 지원자 정보를 생성하거나 업데이트합니다.")
    @PostMapping("/{projectId}/sync-sheet")
    public ResponseEntity<SheetSyncResponse> syncSheet(
            @Parameter(description = "동기화할 프로젝트의 ID", required = true) @PathVariable Long projectId,
            Authentication authentication) {

        Long userId = authService.extractUserId(authentication);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        // 프로젝트에 관한 관리자만 호출 가능
        project.validateAdminAccess(userId);

        // 시트 동기화 실행 (토큰은 GoogleSheetsServiceFactory에서 자동 갱신됨)
        try {
            SheetSyncResponse response = spreadsheetService.syncSheet(projectId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SheetSyncResponse.failure("시트 동기화 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}