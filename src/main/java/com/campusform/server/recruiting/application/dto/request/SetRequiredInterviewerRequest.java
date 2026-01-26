package com.campusform.server.recruiting.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 필수 면접관 개별 설정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SetRequiredInterviewerRequest {

    /**
     * 필수 면접관 여부 (true: 추가, false: 제거)
     */
    @NotNull(message = "필수 면접관 여부는 필수입니다.")
    private Boolean required;
}
