package com.campusform.server.project.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시트 연동 결과 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SheetSyncResponse {

    // 동기화 상태 (SUCCESS, FAILED)
    private String status;

    // 동기화된 지원자 수
    private Integer syncedCount;

    // 결과 메시지
    private String message;

    public static SheetSyncResponse success(Integer syncedCount) {
        return new SheetSyncResponse("SUCCESS", syncedCount, "시트 동기화가 성공적으로 완료되었습니다.");
    }

    public static SheetSyncResponse failure(String message) {
        return new SheetSyncResponse("FAILED", 0, message);
    }
}
