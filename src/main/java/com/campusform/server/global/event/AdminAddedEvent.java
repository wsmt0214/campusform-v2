package com.campusform.server.global.event;

/**
 * 관리자 추가 이벤트
 *
 * Project Context에서 프로젝트에 새로운 관리자가 추가되었을 때 발행됩니다.
 * Notification Context에서 이 이벤트를 수신하여 추가된 관리자에게 알림을 생성합니다.
 *
 * @param projectId     프로젝트 ID
 * @param addedAdminId  새로 추가된 관리자 ID
 * @param projectTitle  프로젝트 제목 (알림 메시지용)
 */
public record AdminAddedEvent(
        Long projectId,
        Long addedAdminId,
        String projectTitle
) {}
