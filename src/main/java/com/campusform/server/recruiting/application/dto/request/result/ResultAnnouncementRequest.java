package com.campusform.server.recruiting.application.dto.request.result;

import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "결과 통보 요청")
public record ResultAnnouncementRequest(
        @Schema(description = "프로젝트 ID")
        @NotNull Long projectId,

        @Schema(description = "결과를 통보할 지원자 ID 목록")
        @NotNull List<Long> applicantIds,

        @Schema(description = "통보할 상태 (PASS 또는 FAIL)")
        @NotNull ScreeningResult status,

        @Schema(description = "결과를 통보할 모집 단계 (DOCUMENT 또는 INTERVIEW)")
        @NotNull String stage
) {
}
