package com.campusform.server.global.event.sheet;

/**
 * 시트 동기화 통계 정보
 */
public record SheetSyncStatistics(
        int totalSyncedCount,
        int newApplicantCount,
        int updatedApplicantCount) {

    /**
     * 변경사항이 있는지 여부
     */
    public boolean hasChanges() {
        return newApplicantCount > 0 || updatedApplicantCount > 0;
    }
}
