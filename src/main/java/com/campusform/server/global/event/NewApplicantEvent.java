package com.campusform.server.global.event;

import java.util.List;

/**
 * 새 지원자 유입 이벤트
 *
 * Recruiting Context에서 새로운 지원자가 등록되었을 때 발행됩니다.
 * Notification Context에서 이 이벤트를 수신하여 관리자들에게 알림을 생성합니다.
 *
 * @param projectId     프로젝트 ID
 * @param adminIds      알림 수신자 목록 (OWNER + ADMIN)
 * @param applicantName 지원자 이름
 */
public record NewApplicantEvent(
        Long projectId,
        List<Long> adminIds,
        String applicantName
) {}
