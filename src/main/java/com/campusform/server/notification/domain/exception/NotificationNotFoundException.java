package com.campusform.server.notification.domain.exception;

import org.springframework.http.HttpStatus;

import com.campusform.server.global.exception.BaseException;

/**
 * 알림 조회 실패 시 발생하는 예외
 * HTTP 404 Not Found로 매핑됨
 */
public class NotificationNotFoundException extends BaseException {

    public NotificationNotFoundException(String message) {
        super(message);
    }

    public NotificationNotFoundException(Long notificationId) {
        super("존재하지 않는 알림입니다. id=" + notificationId);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getErrorCode() {
        return "NOTIFICATION_NOT_FOUND";
    }
}
