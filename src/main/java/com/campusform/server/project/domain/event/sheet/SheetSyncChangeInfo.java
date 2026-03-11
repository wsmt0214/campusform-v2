package com.campusform.server.project.domain.event.sheet;

import java.util.List;

/**
 * 시트 동기화 변경사항 정보
 */
public record SheetSyncChangeInfo(
        Long applicantId,
        String applicantName,
        ChangeType changeType, // 변경 유형 (NEW: 새 행 추가, UPDATED: 기존 행 데이터 변경)
        List<String> changedFieldHeaders) { // 변경된 필드의 헤더 텍스트 리스트 (변경된 필드가 없으면 빈 리스트)
}
