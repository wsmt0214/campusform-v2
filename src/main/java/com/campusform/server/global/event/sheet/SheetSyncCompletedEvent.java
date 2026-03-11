package com.campusform.server.global.event.sheet;

import java.util.List;

/**
 * 시트 동기화 완료 이벤트
 */
public record SheetSyncCompletedEvent(
        Long projectId,
        String projectTitle,
        List<Long> adminIds,
        SheetSyncStatistics statistics,
        List<SheetSyncChangeInfo> changes) {

    /**
     * 동기화 성공 여부 — 통계 정보가 있으면 성공으로 간주
     */
    public boolean isSuccess() {
        return statistics != null;
    }

    /**
     * 하위 호환성을 위한 생성자 (통계 정보만으로 생성)
     */
    public SheetSyncCompletedEvent(Long projectId, List<Long> adminIds, SheetSyncStatistics statistics) {
        this(projectId, null, adminIds, statistics, null);
    }
}
