package com.campusform.server.notification.application.eventhandler;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.campusform.server.project.domain.event.AdminAddedEvent;
import com.campusform.server.recruiting.domain.event.CommentCreatedEvent;
import com.campusform.server.recruiting.domain.event.NewApplicantEvent;
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
     * - 오너에게만: "프로젝트에 새 관리자가 추가되었습니다"
     * - 추가된 관리자(오너 제외)에게만: "해당 프로젝트의 관리자로 추가되었습니다"
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleAdminAdded(AdminAddedEvent event) {
        log.info("관리자 추가 이벤트 수신 - projectId: {}, ownerId: {}, addedAdminIds: {}",
                event.projectId(), event.ownerId(), event.addedAdminIds());

        Long ownerId = event.ownerId();

        // 오너에게만 1건: 오너용 메시지 (수신자 = ownerId만)
        try {
            String ownerPayload = createOwnerAdminAddedPayload(event);
            notificationService.createNotification(
                    ownerId, event.projectId(),
                    NotificationType.ADMIN_ADDED, ownerPayload);
        } catch (Exception e) {
            log.error("관리자 추가 알림 생성 실패(오너) - ownerId: {}, projectId: {}, reason: {}",
                    ownerId, event.projectId(), e.getMessage(), e);
        }

        // 추가된 관리자에게만 각 1건: 관리자용 메시지 (오너와 동일인 제외)
        String adminPayload = createAddedAdminPayload(event);
        for (Long adminId : event.addedAdminIds()) {
            if (ownerId.equals(adminId)) {
                continue; // 오너는 위에서 이미 오너 메시지로 받음, 중복 방지
            }
            try {
                notificationService.createNotification(
                        adminId, event.projectId(),
                        NotificationType.ADMIN_ADDED, adminPayload);
            } catch (Exception e) {
                log.error("관리자 추가 알림 생성 실패(추가된 관리자) - addedAdminId: {}, projectId: {}, reason: {}",
                        adminId, event.projectId(), e.getMessage(), e);
            }
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

    private record AdminAddedPayload(String message, String projectTitle) {
    }

    private record NewApplicantPayload(String message, String applicantName) {
    }

    private record CommentCreatedPayload(String projectTitle, String title, String message, Long applicantId,
            Long commenterId, String stage) {
    }

    // ============ Payload Creation Methods ============

    /** 오너용: 프로젝트에 새 관리자가 추가되었음을 안내 */
    private String createOwnerAdminAddedPayload(AdminAddedEvent event) {
        String message = String.format("'%s' 프로젝트에 새 관리자가 추가되었습니다.", event.projectTitle());
        return toJson(new AdminAddedPayload(message, event.projectTitle()));
    }

    /** 추가된 관리자용: 해당 프로젝트의 관리자로 위임되었음을 안내 */
    private String createAddedAdminPayload(AdminAddedEvent event) {
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
                event.commenterId(),
                event.stage()));
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