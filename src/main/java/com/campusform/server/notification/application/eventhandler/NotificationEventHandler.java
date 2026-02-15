package com.campusform.server.notification.application.eventhandler;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.campusform.server.global.event.AdminAddedEvent;
import com.campusform.server.global.event.CommentCreatedEvent;
import com.campusform.server.global.event.NewApplicantEvent;
import com.campusform.server.notification.application.service.NotificationService;
import com.campusform.server.notification.domain.model.value.NotificationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 이벤트 핸들러
 *
 * 다른 Context에서 발행된 도메인 이벤트를 수신하여 알림을 생성합니다.
 * 비동기 처리(@Async)를 통해 이벤트 발행자에게 영향을 주지 않습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * 관리자 추가 이벤트 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleAdminAdded(AdminAddedEvent event) {
        log.info("관리자 추가 이벤트 수신 - projectId: {}, addedAdminId: {}",
                event.projectId(), event.addedAdminId());

        String payload = createAdminAddedPayload(event);
        try {
            notificationService.createNotification(
                    event.addedAdminId(), event.projectId(),
                    NotificationType.ADMIN_ADDED, payload);
        } catch (Exception e) {
            log.error("관리자 추가 알림 생성 실패 - adminId: {}, projectId: {}",
                    event.addedAdminId(), event.projectId(), e);
        }
    }

    /**
     * 새 지원자 유입 이벤트 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleNewApplicant(NewApplicantEvent event) {
        log.info("새 지원자 유입 이벤트 수신 - projectId: {}, applicantName: {}",
                event.projectId(), event.applicantName());

        String payload = createNewApplicantPayload(event);
        for (Long adminId : event.adminIds()) {
            try {
                notificationService.createNotification(
                        adminId, event.projectId(),
                        NotificationType.NEW_APPLICANT, payload);
            } catch (Exception e) {
                log.error("새 지원자 알림 생성 실패 - adminId: {}, projectId: {}",
                        adminId, event.projectId(), e);
            }
        }
    }

    /**
     * 댓글 생성 이벤트 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleCommentCreated(CommentCreatedEvent event) {
        log.info("댓글 생성 이벤트 수신 - projectId: {}, applicantId: {}, commenterId: {}",
                event.projectId(), event.applicantId(), event.commenterId());

        String payload = createCommentPayload(event);
        for (Long recipientId : event.recipientIds()) {
            try {
                notificationService.createNotification(
                        recipientId, event.projectId(),
                        NotificationType.COMMENT_CREATED, payload);
            } catch (Exception e) {
                log.error("댓글 알림 생성 실패 - recipientId: {}, projectId: {}",
                        recipientId, event.projectId(), e);
            }
        }
    }

    // ============ Payload Records ============

    private record SheetSyncPayload(String message, int syncedCount) {
    }

    private record AdminAddedPayload(String message, String projectTitle) {
    }

    private record NewApplicantPayload(String message, String applicantName) {
    }

    private record CommentCreatedPayload(String projectTitle, String title, String message, Long applicantId,
            Long commenterId) {
    }

    // ============ Payload Creation Methods ============

    private String createAdminAddedPayload(AdminAddedEvent event) {
        String message = String.format("'%s' 프로젝트의 관리자로 추가되었습니다.", event.projectTitle());
        return toJson(new AdminAddedPayload(message, event.projectTitle()));
    }

    private String createNewApplicantPayload(NewApplicantEvent event) {
        String message = String.format("새로운 지원자 '%s' 님이 지원했습니다.", event.applicantName());
        return toJson(new NewApplicantPayload(message, event.applicantName()));
    }

    private String createCommentPayload(CommentCreatedEvent event) {
        String projectTitle = event.projectTitle() != null ? event.projectTitle().trim() : null;
        String title = String.format("%s 님의 지원서", event.applicantName());
        String message = String.format("%s 님이 댓글을 작성했습니다.", event.commenterName());
        return toJson(new CommentCreatedPayload(
                projectTitle,
                title,
                message,
                event.applicantId(),
                event.commenterId()));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Payload JSON 직렬화 실패", e);
            return "{}";
        }
    }
}