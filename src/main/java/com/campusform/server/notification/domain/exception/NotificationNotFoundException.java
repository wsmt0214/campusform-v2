package com.campusform.server.notification.domain.exception;

/**
 * 알림 조회 실패 시 발생하는 예외
 * HTTP 404 Not Found로 매핑됩니다.
 */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(String message) {
        super(message);
    }

    public NotificationNotFoundException(Long notificationId) {
        super("존재하지 않는 알림입니다. id=" + notificationId);
    }
}
