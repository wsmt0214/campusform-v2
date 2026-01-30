package com.campusform.server.notification.presentation;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
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
    private final AuthService authService;

    /**
     * 알림 목록 조회 (로그인한 사용자 본인의 것)
     */
    @GetMapping
    public NotificationListResponse getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // userId 추출 로직은 한 곳(AuthService)으로 모아서 재사용/일관성 유지
        Long userId = authService.extractUserId(authentication);
        return notificationService.getNotifications(userId, page, size);
    }

    /**
     * 알림 읽음 처리 (로그인한 사용자 본인의 것)
     */
    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = authService.extractUserId(authentication);
        return notificationService.markAsRead(id, userId);
    }

    /**
     * 안읽은 알림 개수 조회 (로그인한 사용자 본인의 것)
     */
    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(Authentication authentication) {

        Long userId = authService.extractUserId(authentication);
        return notificationService.getUnreadCount(userId);
    }

    /**
     * 모든 알림 읽음 처리 (로그인한 사용자 본인의 것)
     */
    @PatchMapping("/read-all")
    public MarkAllAsReadResponse markAllAsRead(Authentication authentication) {

        Long userId = authService.extractUserId(authentication);
        int count = notificationService.markAllAsRead(userId);
        return new MarkAllAsReadResponse(count);
    }
}
