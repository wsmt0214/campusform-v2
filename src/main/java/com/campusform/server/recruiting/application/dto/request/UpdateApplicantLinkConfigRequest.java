package com.campusform.server.recruiting.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원자 페이지 설정 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicantLinkConfigRequest {

    /**
     * 응답 받기 ON/OFF (null이면 수정하지 않음)
     */
    private Boolean enabled;

    /**
     * 안내사항 문구 (null이면 수정하지 않음)
     */
    private String guidanceText;
}
