package com.campusform.server.notification.application.dto.response;

import java.time.LocalDateTime;

import com.campusform.server.notification.domain.model.Notification;
import com.campusform.server.notification.domain.model.value.NotificationType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 응답 DTO
 */
@Schema(description = "알림 상세 정보 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    @Schema(description = "알림 ID", example = "101")
    private Long id;
    @Schema(description = "관련 프로젝트 ID", example = "1")
    private Long projectId;
    @Schema(description = "알림 종류")
    private NotificationType type;
    @Schema(description = "알림 내용 (JSON 문자열)", example = "{\"commenter\":\"김관리\",\"content\":\"확인 바랍니다.\"}")
    private String payload;
    @Schema(description = "읽음 여부", example = "false")
    private boolean read;
    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;
    @Schema(description = "읽음 처리 시각 (읽지 않았으면 null)")
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
