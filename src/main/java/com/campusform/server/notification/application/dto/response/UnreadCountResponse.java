package com.campusform.server.notification.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 안읽은 알림 개수 응답 DTO
 */
@Schema(description = "안 읽은 알림 개수 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {

    @Schema(description = "안 읽은 알림 개수", example = "12")
    private long unreadCount;

    public static UnreadCountResponse of(long count) {
        return new UnreadCountResponse(count);
    }
}
