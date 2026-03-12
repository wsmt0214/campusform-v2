package com.campusform.server.project.presentation;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.campusform.server.global.security.CurrentUserId;
import com.campusform.server.project.application.dto.response.PositionValuesResponse;
import com.campusform.server.project.application.dto.response.SheetSyncResponse;
import com.campusform.server.project.application.dto.response.SpreadsheetColumnResponse;
import com.campusform.server.project.application.service.ProjectAccessService;
import com.campusform.server.project.application.service.SpreadsheetService;
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
    private final ProjectAccessService projectAccessService;

    /**
     * 시트 헤더 조회 API
     */
    @Operation(summary = "Google Sheet 헤더(컬럼) 조회", description = "주어진 Google Sheet URL의 첫 번째 행(헤더)에 있는 컬럼 목록을 조회합니다.")
    @GetMapping("/sheet-headers")
    public ResponseEntity<List<SpreadsheetColumnResponse>> getSheetHeaders(
            @Parameter(description = "헤더를 조회할 Google Sheet의 전체 URL", required = true) @RequestParam String sheetUrl,
            @CurrentUserId Long userId) {
        List<SpreadsheetColumnResponse> headers = spreadsheetService.getSheetHeaders(sheetUrl, userId);
        return ResponseEntity.ok(headers);
    }

    /**
     * 포지션 컬럼 고유값 목록 조회 (편집하기용)
     */
    @Operation(summary = "포지션 컬럼 고유값 목록 조회", description = "지정한 시트의 지정 컬럼 인덱스에 등장하는 모든 고유값을 반환합니다.")
    @GetMapping("/mapping-column-values")
    public ResponseEntity<PositionValuesResponse> getPositionValues(
            @Parameter(description = "시트 URL", required = true) @RequestParam String sheetUrl,
            @Parameter(description = "포지션 컬럼의 인덱스 (-1, 즉 미매핑 시 빈 List를 반환)", required = true) @RequestParam Integer positionColumnIndex,
            @CurrentUserId Long userId) {
        List<String> values = spreadsheetService.getDistinctPositionValues(sheetUrl, userId, positionColumnIndex);
        return ResponseEntity.ok(PositionValuesResponse.from(values));
    }

    /**
     * 시트 동기화 API
     */
    @Operation(summary = "Google Sheet와 지원자 정보 동기화", description = "프로젝트에 연결된 Google Sheet의 데이터를 읽어와 지원자 정보를 생성하거나 업데이트합니다.")
    @PostMapping("/{projectId}/sync-sheet")
    public ResponseEntity<SheetSyncResponse> syncSheet(
            @Parameter(description = "동기화할 프로젝트의 ID", required = true) @PathVariable Long projectId,
            @CurrentUserId Long userId) {
        projectAccessService.getProjectWithAdminAccess(projectId, userId);

        try {
            SheetSyncResponse response = spreadsheetService.syncSheet(projectId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SheetSyncResponse.failure("시트 동기화 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
