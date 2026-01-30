package com.campusform.server.recruiting.application.dto.request;

import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import com.campusform.server.recruiting.domain.model.applicant.value.StageStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SmsTemplateSaveRequest {
    @NotNull(message = "단계는 필수입니다.")
    private StageStatus stage;   // "DOCUMENT", "INTERVIEW"

    @NotNull(message="상태값은 필수입니다.")
    private ApplicantStatus status;  // "PASS" , "FAIL", "HOLD"

    @NotBlank(message = "문자 내용은 필수입니다.")
    private String content; // "안녕하세요 [요리퐁]입니다..."
}
