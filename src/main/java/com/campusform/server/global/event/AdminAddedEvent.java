package com.campusform.server.global.event;

import java.util.List;

/**
 * 관리자 추가 이벤트
 *
 * Project Context에서 프로젝트에 새로운 관리자가 추가되었을 때 발행됩니다.
 * Notification Context에서 이 이벤트를 수신하여 오너 + 추가된 관리자 각각에게 알림을 생성합니다.
 *
 * @param projectId      프로젝트 ID
 * @param ownerId        프로젝트 오너 ID
 * @param addedAdminIds  새로 추가된 관리자 ID 목록
 * @param projectTitle   프로젝트 제목 (알림 메시지용)
 */
public record AdminAddedEvent(
        Long projectId,
        Long ownerId,
        List<Long> addedAdminIds,
        String projectTitle
) {}
