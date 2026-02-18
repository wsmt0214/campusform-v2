package com.campusform.server.project.application.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 모집 기간(시작일·종료일) 수정 요청 DTO
 */
@Schema(description = "프로젝트 모집 기간 수정 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectPeriodRequest {

    @Schema(description = "모집 시작일", example = "2024-03-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "모집 시작일은 필수입니다.")
    private LocalDate startAt;

    @Schema(description = "모집 종료일", example = "2024-03-15", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "모집 종료일은 필수입니다.")
    private LocalDate endAt;

    /**
     * 종료일이 시작일보다 이전이면 안 됨 (Bean Validation용)
     */
    @AssertTrue(message = "모집 종료일은 시작일 이후여야 합니다.")
    @Schema(hidden = true)
    public boolean isEndAtNotBeforeStartAt() {
        if (startAt == null || endAt == null) {
            return true; // null은 @NotNull에서 처리
        }
        return !endAt.isBefore(startAt);
    }
}
