package com.campusform.server.notification.application.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.campusform.server.notification.domain.model.Notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 목록 응답 DTO (페이징 정보 포함)
 */
@Schema(description = "알림 목록 페이징 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponse {

    @Schema(description = "알림 목록")
    private List<NotificationResponse> notifications;
    @Schema(description = "현재 페이지 번호", example = "0")
    private int currentPage;
    @Schema(description = "전체 페이지 수", example = "10")
    private int totalPages;
    @Schema(description = "전체 알림 개수", example = "95")
    private long totalElements;
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;
    @Schema(description = "이전 페이지 존재 여부", example = "false")
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
