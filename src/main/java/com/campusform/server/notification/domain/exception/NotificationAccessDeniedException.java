package com.campusform.server.notification.domain.exception;

import org.springframework.http.HttpStatus;
import com.campusform.server.global.exception.BaseException;
import lombok.Getter;

/**
 * 알림 접근 권한이 없을 때 발생하는 예외
 * HTTP 403 Forbidden으로 매핑됨
 */
@Getter
public class NotificationAccessDeniedException extends BaseException {

    private static final String CLIENT_MESSAGE = "해당 알림에 대한 접근 권한이 없습니다.";

    private final Long notificationId;
    private final Long userId;

    public NotificationAccessDeniedException(Long notificationId, Long userId) {
        super(CLIENT_MESSAGE);
        this.notificationId = notificationId;
        this.userId = userId;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }

    @Override
    public String getErrorCode() {
        return "NOTIFICATION_ACCESS_DENIED";
    }

    /**
     * 로깅용 상세 메시지
     */
    @Override
    public String getDetailMessage() {
        return String.format("%s notificationId=%d, userId=%d", CLIENT_MESSAGE, notificationId, userId);
    }
}
