package com.campusform.server.notification.application.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.campusform.server.notification.domain.model.Notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 목록 응답 DTO (페이징 정보 포함)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponse {

    private List<NotificationResponse> notifications;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
    private boolean hasPrevious;

    public static NotificationListResponse from(Page<Notification> notificationPage) {
        List<NotificationResponse> notifications = notificationPage.getContent()
                .stream()
                .map(NotificationResponse::from)
                .toList();

        return new NotificationListResponse(
                notifications,
                notificationPage.getNumber(),
                notificationPage.getTotalPages(),
                notificationPage.getTotalElements(),
                notificationPage.hasNext(),
                notificationPage.hasPrevious()
        );
    }
}
