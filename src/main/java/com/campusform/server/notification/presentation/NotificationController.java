package com.campusform.server.notification.presentation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.notification.application.dto.response.MarkAllAsReadResponse;
import com.campusform.server.notification.application.dto.response.NotificationListResponse;
import com.campusform.server.notification.application.dto.response.NotificationResponse;
import com.campusform.server.notification.application.dto.response.UnreadCountResponse;
import com.campusform.server.notification.application.service.NotificationService;

import lombok.RequiredArgsConstructor;

/**
 * 알림 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회 (로그인한 사용자 본인의 것)
     */
    @GetMapping
    public NotificationListResponse getNotifications(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = oauth2User.getAttribute("userId");
        return notificationService.getNotifications(userId, page, size);
    }

    /**
     * 알림 읽음 처리 (로그인한 사용자 본인의 것)
     */
    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        Long userId = oauth2User.getAttribute("userId");
        return notificationService.markAsRead(id, userId);
    }

    /**
     * 안읽은 알림 개수 조회 (로그인한 사용자 본인의 것)
     */
    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(@AuthenticationPrincipal OAuth2User oauth2User) {

        Long userId = oauth2User.getAttribute("userId");
        return notificationService.getUnreadCount(userId);
    }

    /**
     * 모든 알림 읽음 처리 (로그인한 사용자 본인의 것)
     */
    @PatchMapping("/read-all")
    public MarkAllAsReadResponse markAllAsRead(@AuthenticationPrincipal OAuth2User oauth2User) {

        Long userId = oauth2User.getAttribute("userId");
        int count = notificationService.markAllAsRead(userId);
        return new MarkAllAsReadResponse(count);
    }
}
