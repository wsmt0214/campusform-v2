package com.campusform.server.identity.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 수신 설정 변경 요청")
public record UpdateNotificationSettingRequest(
    @Schema(description = "알림 수신 여부", example = "true")
    boolean enabled
) {}
