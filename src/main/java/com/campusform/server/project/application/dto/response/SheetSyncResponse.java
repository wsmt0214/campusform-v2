package com.campusform.server.project.application.dto.response;

import java.util.List;

import com.campusform.server.global.event.ChangeType;

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

    @Schema(description = "변경사항 목록 (새 행 추가, 데이터 변경)", example = "[]")
    private List<ApplicantChangeInfo> changes;

    /**
     * 지원자 변경사항 정보
     */
    @Schema(description = "지원자 변경사항 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantChangeInfo {
        @Schema(description = "지원자 ID", example = "1")
        private Long applicantId;

        @Schema(description = "지원자 이름", example = "홍길동")
        private String applicantName;

        @Schema(description = "변경 유형", example = "NEW")
        private ChangeType changeType;

        @Schema(description = "변경된 필드의 헤더 텍스트 리스트", example = "[\"전화번호\", \"학교\"]")
        private List<String> changedFieldHeaders;
    }

    public static SheetSyncResponse success(Integer syncedCount) {
        return new SheetSyncResponse("SUCCESS", syncedCount, "시트 동기화가 성공적으로 완료되었습니다.", null);
    }

    public static SheetSyncResponse success(Integer syncedCount, List<ApplicantChangeInfo> changes) {
        return new SheetSyncResponse("SUCCESS", syncedCount, "시트 동기화가 성공적으로 완료되었습니다.", changes);
    }

    public static SheetSyncResponse failure(String message) {
        return new SheetSyncResponse("FAILED", 0, message, null);
    }
}