package com.campusform.server.project.domain.event;

import java.util.List;

/**
 * 관리자 추가 이벤트
 *
 * Project Context에서 프로젝트에 새로운 관리자가 추가되었을 때 발행 
 */
public record AdminAddedEvent(
        Long projectId,
        Long ownerId,
        List<Long> addedAdminIds,
        String projectTitle
) {}
