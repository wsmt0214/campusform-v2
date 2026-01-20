package com.campusform.server.notification.application.dto.response;

import java.time.LocalDateTime;

import com.campusform.server.notification.domain.model.Notification;
import com.campusform.server.notification.domain.model.value.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long projectId;
    private NotificationType type;
    private String payload;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getProjectId(),
                notification.getType(),
                notification.getPayload(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
