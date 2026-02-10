package com.campusform.server.recruiting.application.dto.request;

import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "SMS 템플릿 저장 요청")
@Getter
@NoArgsConstructor
public class SmsTemplateSaveRequest {
    @Schema(description = "템플릿을 적용할 모집 단계 (DOCUMENT 또는 INTERVIEW)", example = "DOCUMENT")
    @NotNull(message = "단계는 필수입니다.")
    private RecruitmentStage stage;   // "DOCUMENT", "INTERVIEW"

    @Schema(description = "템플릿을 적용할 지원자 상태 (PASS 또는 FAIL)", example = "PASS")
    @NotNull(message="상태값은 필수입니다.")
    private ScreeningResult status;  // "PASS" , "FAIL", "HOLD"

    @Schema(description = "SMS 템플릿 내용. @이름은 지원자 이름으로, @포지션은 지원 포지션으로 자동 치환됩니다.", example = "안녕하세요, @이름님. 서류 전형에 합격하셨습니다. (포지션: @포지션)")
    @NotBlank(message = "문자 내용은 필수입니다.")
    private String content; // "안녕하세요 [요리퐁]입니다..."
}
