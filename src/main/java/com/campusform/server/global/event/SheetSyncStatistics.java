package com.campusform.server.global.event;

/**
 * 시트 동기화 통계 정보
 * 
 * 변경사항에 대한 통계를 제공하여 이벤트 핸들러에서 계산할 필요를 없앱니다.
 * 
 * @param totalSyncedCount      전체 동기화된 지원자 수
 * @param newApplicantCount     새로 추가된 지원자 수
 * @param updatedApplicantCount 정보가 업데이트된 지원자 수
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
