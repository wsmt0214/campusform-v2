package com.campusform.server.global.event;

import java.util.List;

/**
 * 댓글 생성 이벤트
 *
 * Recruiting Context에서 지원자에 대한 댓글이 생성되었을 때 발행됩니다.
 * Notification Context에서 이 이벤트를 수신하여 다른 관리자들에게 알림을 생성합니다.
 *
 * @param projectId      프로젝트 ID
 * @param projectTitle   프로젝트 제목 (알림 title 앞에 표시용)
 * @param applicantId    지원자 ID
 * @param applicantName  지원자 이름
 * @param commenterId    댓글 작성자 ID
 * @param commenterName  댓글 작성자 이름
 * @param recipientIds   알림 수신자 목록 (댓글 작성자 제외)
 */
public record CommentCreatedEvent(
        Long projectId,
        String projectTitle,
        Long applicantId,
        String applicantName,
        Long commenterId,
        String commenterName,
        List<Long> recipientIds
) {}
