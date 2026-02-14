package com.campusform.server.notification.domain.model.value;

/**
 * 알림 타입 Enum
 * 알림의 원인을 나타냅니다.
 * 
 * 특정 트리거에 대한 알림 타입을 임의로 설정했습니다.
 * 향후 수정 필요
 */
public enum NotificationType {
    // /** 시트 동기화 결과 */
    // SHEET_SYNC_RESULT,

    /** 새 지원자 유입 */
    NEW_APPLICANT,

    /** 댓글 생성 */
    COMMENT_CREATED,

    /** 관리자 추가 */
    ADMIN_ADDED
}
