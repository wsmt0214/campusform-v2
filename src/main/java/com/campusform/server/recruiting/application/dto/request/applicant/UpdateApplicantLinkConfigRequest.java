package com.campusform.server.recruiting.application.dto.request.applicant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원자 페이지 설정 수정 요청 DTO
 */
@Schema(description = "지원자 시간 제출 페이지 설정 수정 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicantLinkConfigRequest {

    @Schema(description = "페이지 활성화 여부 (true: 활성화, false: 비활성화)", example = "true")
    private Boolean enabled;

    @Schema(description = "페이지에 표시될 안내 문구", example = "면접 가능하신 시간을 모두 선택해주세요.")
    private String guidanceText;
}
