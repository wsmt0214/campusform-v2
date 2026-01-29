package com.campusform.server.recruiting.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApplicantStatusUpdateResponse {
    private Long applicantId;
    private String currentStatus;
    private LocalDateTime updateAt;
}
