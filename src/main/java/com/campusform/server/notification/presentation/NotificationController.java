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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 알림 관련 API 컨트롤러
 */
@Tag(name = "알림", description = "알림 조회 및 읽음 처리 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    /**
     * 알림 목록 조회 (로그인한 사용자 본인의 것)
     */
    @Operation(summary = "알림 목록 조회", description = "현재 로그인한 사용자의 알림 목록을 최신순으로 페이징하여 조회합니다.")
    @GetMapping
    public NotificationListResponse getNotifications(
            Authentication authentication,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지당 알림 개수") @RequestParam(defaultValue = "20") int size) {

        // userId 추출 로직은 한 곳(AuthService)으로 모아서 재사용/일관성 유지
        Long userId = authService.extractUserId(authentication);
        return notificationService.getNotifications(userId, page, size);
    }

    /**
     * 알림 읽음 처리 (로그인한 사용자 본인의 것)
     */
    @Operation(summary = "알림 단건 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(
            @Parameter(description = "읽음 처리할 알림 ID") @PathVariable Long id,
            Authentication authentication) {

        Long userId = authService.extractUserId(authentication);
        return notificationService.markAsRead(id, userId);
    }

    /**
     * 안읽은 알림 개수 조회 (로그인한 사용자 본인의 것)
     */
    @Operation(summary = "안 읽은 알림 개수 조회", description = "현재 로그인한 사용자의 읽지 않은 알림 개수를 조회합니다.")
    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(Authentication authentication) {

        Long userId = authService.extractUserId(authentication);
        return notificationService.getUnreadCount(userId);
    }

    /**
     * 모든 알림 읽음 처리 (로그인한 사용자 본인의 것)
     */
    @Operation(summary = "모든 알림 읽음 처리", description = "현재 로그인한 사용자의 모든 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/read-all")
    public MarkAllAsReadResponse markAllAsRead(Authentication authentication) {

        Long userId = authService.extractUserId(authentication);
        int count = notificationService.markAllAsRead(userId);
        return new MarkAllAsReadResponse(count);
    }
}
