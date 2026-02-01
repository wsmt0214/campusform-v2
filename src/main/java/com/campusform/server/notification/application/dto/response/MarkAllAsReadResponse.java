package com.campusform.server.notification.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모든 알림 읽음 처리 응답")
public record MarkAllAsReadResponse(
    @Schema(description = "읽음 처리된 알림 개수", example = "5")
    int updatedCount
) {}
