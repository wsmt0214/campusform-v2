package com.campusform.server.recruiting.application.dto.request.applicant;

import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "지원자 상태 변경 요청")
@Getter
@NoArgsConstructor
public class ApplicantStatusUpdateRequest {
    @Schema(description = "변경할 상태", example = "PASS")
    private ScreeningResult status;
}
