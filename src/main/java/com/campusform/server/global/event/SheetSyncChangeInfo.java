package com.campusform.server.global.event;

import java.util.List;

/**
 * 시트 동기화 변경사항 정보
 * 
 * 이벤트 전용 값 객체로, 애플리케이션 DTO와의 의존성을 제거합니다.
 * 
 * @param applicantId         지원자 ID
 * @param applicantName       지원자 이름
 * @param changeType          변경 유형 (NEW: 새 행 추가, UPDATED: 기존 행 데이터 변경)
 * @param changedFieldHeaders 변경된 필드의 헤더 텍스트 리스트 (변경된 필드가 없으면 빈 리스트)
 */
public record SheetSyncChangeInfo(
        Long applicantId,
        String applicantName,
        ChangeType changeType,
        List<String> changedFieldHeaders) {
}
