package com.campusform.server.identity.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 수신 설정 조회 및 변경 응답")
public record NotificationSettingResponse(
    @Schema(description = "알림 수신 여부", example = "true")
    boolean enabled
) {}
