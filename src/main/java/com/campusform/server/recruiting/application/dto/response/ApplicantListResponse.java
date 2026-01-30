package com.campusform.server.recruiting.application.dto.response;

import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ApplicantListResponse {
    private ApplicantStatus status;
    private List<ApplicantResponse> applicants;

    @Getter
    @Builder
    public static class ApplicantStatus {
        private long totalCount;
        private long pendingCount;
        private long passCount;
        private long failCount;
    }
}
