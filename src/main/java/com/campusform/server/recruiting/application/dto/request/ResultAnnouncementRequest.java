package com.campusform.server.recruiting.application.dto.request;

import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ResultAnnouncementRequest(
        @NotNull Long projectId,
        @NotNull List<Long> applicantIds, // 처리할 지원자 ID 목록
        @NotNull ApplicantStatus status, // PASSED or FAILED
        @NotNull String stage // "DOCUMENT" or "INTERVIEW"
) {
}
