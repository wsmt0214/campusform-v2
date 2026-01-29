package com.campusform.server.recruiting.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicantResponse {
    private Long id;
    private String name;
    private String major;     // 학과
    private String phone;     // 전화번호
    private boolean bookmarked; // 찜 여부 (★/☆)
}
