package com.campusform.server.recruiting.application.dto.request.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 필수 면접관 개별 설정 요청 DTO
 */
@Schema(description = "필수 면접관 개별 설정 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SetRequiredInterviewerRequest {

    @Schema(description = "필수 면접관 지정 여부 (true: 지정, false: 해제)", example = "true")
    @NotNull(message = "필수 면접관 여부는 필수입니다.")
    private Boolean required;
}
