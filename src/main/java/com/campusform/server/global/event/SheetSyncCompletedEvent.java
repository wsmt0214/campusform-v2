package com.campusform.server.global.event;

import java.util.List;

/**
 * 시트 동기화 완료 이벤트
 *
 * Project Context에서 스프레드시트 동기화가 완료되었을 때 발행됩니다.
 * Notification Context에서 이 이벤트를 수신하여 관리자들에게 알림을 생성합니다.
 *
 * @param projectId  프로젝트 ID
 * @param adminIds   알림 수신자 목록 (OWNER + ADMIN)
 * @param statistics 동기화 통계 정보
 * @param changes    변경사항 목록 (null 가능 - 변경사항이 없을 경우)
 */
public record SheetSyncCompletedEvent(
        Long projectId,
        List<Long> adminIds,
        SheetSyncStatistics statistics,
        List<SheetSyncChangeInfo> changes) {
    /**
     * 동기화 성공 여부
     * 통계 정보가 있으면 성공으로 간주
     */
    public boolean isSuccess() {
        return statistics != null;
    }

    /**
     * 하위 호환성을 위한 생성자 (통계 정보만으로 생성)
     */
    public SheetSyncCompletedEvent(Long projectId, List<Long> adminIds, SheetSyncStatistics statistics) {
        this(projectId, adminIds, statistics, null);
    }
}