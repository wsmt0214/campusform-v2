package com.campusform.server.project.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시트 연동 결과 응답 DTO
 */
@Schema(description = "Google Sheet 동기화 결과 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SheetSyncResponse {

    @Schema(description = "동기화 상태", example = "SUCCESS")
    // 동기화 상태 (SUCCESS, FAILED)
    private String status;

    @Schema(description = "동기화된 지원자 수", example = "50")
    // 동기화된 지원자 수
    private Integer syncedCount;

    @Schema(description = "결과 메시지", example = "시트 동기화가 성공적으로 완료되었습니다.")
    // 결과 메시지
    private String message;

    public static SheetSyncResponse success(Integer syncedCount) {
        return new SheetSyncResponse("SUCCESS", syncedCount, "시트 동기화가 성공적으로 완료되었습니다.");
    }

    public static SheetSyncResponse failure(String message) {
        return new SheetSyncResponse("FAILED", 0, message);
    }
}
